package com.shop.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.coupon.dto.RepurchaseCouponVO;
import com.shop.coupon.dto.RepurchaseIssueResult;
import com.shop.coupon.entity.CouponIssueRecord;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.entity.UserCoupon;
import com.shop.coupon.enums.CouponIssueRecordStatus;
import com.shop.coupon.enums.CouponIssueScene;
import com.shop.coupon.enums.UserCouponStatus;
import com.shop.coupon.mapper.CouponIssueRecordMapper;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.coupon.service.CouponIssueService;
import com.shop.coupon.service.CouponService;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueServiceImpl implements CouponIssueService {
    private static final int TARGET_ALL = 0;
    private static final int TARGET_PRODUCT = 1;
    private static final int TARGET_CATEGORY = 2;

    private final CouponTemplateMapper templateMapper;
    private final CouponIssueRecordMapper recordMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final CouponService couponService;
    private final MarketingFeatureService marketingFeatureService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueAfterPaid(Order order) {
        if (order == null || order.getId() == null || order.getUserId() == null || order.getMerchantId() == null
                || order.getOrderNo() == null || order.getPayTime() == null || isActivityOrder(order)) return;
        if (!marketingFeatureService.isEnabled(order.getMerchantId(), MarketingActivityCode.REPURCHASE_COUPON)) return;

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        Map<Long, Product> products = products(items);
        List<CouponTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getMerchantId, order.getMerchantId())
                        .eq(CouponTemplate::getIssueScene, CouponIssueScene.REPURCHASE_AFTER_PAID)
                        .eq(CouponTemplate::getStatus, 1)
                        .orderByDesc(CouponTemplate::getRepurchasePriority)
                        .orderByAsc(CouponTemplate::getId));
        for (CouponTemplate template : templates) {
            if (!matches(template, order, items, products)) continue;
            issueForTemplate(order, template);
            return; // P1: 每笔订单至多赠送一张复购券。
        }
    }

    private void issueForTemplate(Order order, CouponTemplate template) {
        String idempotencyKey = order.getOrderNo() + ":" + template.getId();
        CouponIssueRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<CouponIssueRecord>()
                .eq(CouponIssueRecord::getIdempotencyKey, idempotencyKey).last("FOR UPDATE"));
        if (existing != null) return;

        CouponIssueRecord record = new CouponIssueRecord();
        record.setMerchantId(order.getMerchantId());
        record.setUserId(order.getUserId());
        record.setSourceOrderNo(order.getOrderNo());
        record.setTemplateId(template.getId());
        record.setIssueScene(CouponIssueScene.REPURCHASE_AFTER_PAID);
        record.setStatus(CouponIssueRecordStatus.PENDING.getCode());
        record.setIdempotencyKey(idempotencyKey);
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            return;
        }

        RepurchaseIssueResult result = couponService.issueRepurchaseCoupon(
                order.getUserId(), order.getMerchantId(), template.getId(), order.getOrderNo());
        record.setUserCouponId(result.getCouponId());
        if (result.isIssued()) {
            record.setStatus(CouponIssueRecordStatus.ISSUED.getCode());
        } else {
            record.setStatus(CouponIssueRecordStatus.SKIPPED.getCode());
            record.setSkipReason("用户已获得过该复购券");
        }
        recordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void revokeAfterFullRefund(Order order, RefundApplication refund) {
        if (order == null || refund == null) return;
        List<CouponIssueRecord> records = recordMapper.selectList(new LambdaQueryWrapper<CouponIssueRecord>()
                .eq(CouponIssueRecord::getMerchantId, order.getMerchantId())
                .eq(CouponIssueRecord::getSourceOrderNo, order.getOrderNo())
                .eq(CouponIssueRecord::getIssueScene, CouponIssueScene.REPURCHASE_AFTER_PAID));
        for (CouponIssueRecord record : records) {
            if (record.getStatus() == CouponIssueRecordStatus.REVOKED.getCode()
                    || record.getStatus() == CouponIssueRecordStatus.REFUND_CANCELLED.getCode()) continue;
            if (record.getUserCouponId() == null) {
                record.setStatus(CouponIssueRecordStatus.REFUND_CANCELLED.getCode());
                record.setRefundId(refund.getId());
                record.setRevokedAt(LocalDateTime.now());
                recordMapper.updateById(record);
                continue;
            }
            boolean revoked = couponService.invalidateCoupon(order.getUserId(), order.getMerchantId(), record.getUserCouponId(),
                    "原订单已全额退款，优惠券已失效");
            if (revoked) {
                record.setStatus(CouponIssueRecordStatus.REVOKED.getCode());
                record.setRefundId(refund.getId());
                record.setRevokedAt(LocalDateTime.now());
                recordMapper.updateById(record);
            }
        }
    }

    @Override
    public RepurchaseCouponVO findRepurchaseCoupon(Long userId, Long merchantId, String orderNo) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REPURCHASE_COUPON);
        CouponIssueRecord record = recordMapper.selectOne(new LambdaQueryWrapper<CouponIssueRecord>()
                .eq(CouponIssueRecord::getMerchantId, merchantId)
                .eq(CouponIssueRecord::getUserId, userId)
                .eq(CouponIssueRecord::getSourceOrderNo, orderNo)
                .eq(CouponIssueRecord::getIssueScene, CouponIssueScene.REPURCHASE_AFTER_PAID)
                .orderByDesc(CouponIssueRecord::getId).last("LIMIT 1"));
        if (record == null || record.getUserCouponId() == null) return null;
        UserCoupon coupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, record.getUserCouponId())
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getMerchantId, merchantId));
        if (coupon == null) return null;
        RepurchaseCouponVO vo = new RepurchaseCouponVO();
        vo.setId(coupon.getId()); vo.setTemplateId(coupon.getTemplateId()); vo.setName(coupon.getTemplateNameSnapshot());
        vo.setAmount(coupon.getAmountSnapshot()); vo.setThresholdAmount(coupon.getThresholdSnapshot());
        vo.setValidFrom(coupon.getValidFrom()); vo.setValidTo(coupon.getValidTo()); vo.setStatus(coupon.getStatus());
        vo.setStatusText(statusText(coupon)); vo.setIssueScene(coupon.getIssueScene()); vo.setSourceOrderNo(coupon.getSourceOrderNo());
        vo.setUnavailableReason(coupon.getInvalidReason());
        vo.setAvailable(coupon.getStatus() == UserCouponStatus.WAIT_USE.getCode()
                && coupon.getValidTo() != null && !LocalDateTime.now().isAfter(coupon.getValidTo()));
        vo.setIssueStatus(record.getStatus().equals(CouponIssueRecordStatus.ISSUED.getCode()) ? "ISSUED" : "UNAVAILABLE");
        return vo;
    }

    private boolean matches(CouponTemplate template, Order order, List<OrderItem> items, Map<Long, Product> products) {
        if (template.getValidFrom() != null && LocalDateTime.now().isBefore(template.getValidFrom())) return false;
        if (template.getValidTo() != null && LocalDateTime.now().isAfter(template.getValidTo())) return false;
        if (template.getRepurchaseMinOrderAmount() != null
                && Optional.ofNullable(order.getPayAmount()).orElse(BigDecimal.ZERO).compareTo(template.getRepurchaseMinOrderAmount()) < 0) return false;
        if (Integer.valueOf(1).equals(template.getRepurchaseFirstPurchaseOnly()) && previousSuccessfulOrderCount(order) != 0) return false;
        Set<Long> targetIds = parseIds(template.getRepurchaseTargetIdsJson());
        int targetType = Optional.ofNullable(template.getRepurchaseTargetType()).orElse(TARGET_ALL);
        if (targetType == TARGET_ALL) return true;
        if (targetIds.isEmpty()) return false;
        if (targetType == TARGET_PRODUCT) return items.stream().anyMatch(item -> targetIds.contains(item.getProductId()));
        return items.stream().map(item -> products.get(item.getProductId())).filter(Objects::nonNull)
                .anyMatch(product -> targetIds.contains(product.getCategoryId()));
    }

    private Map<Long, Product> products(List<OrderItem> items) {
        List<Long> ids = items.stream().map(OrderItem::getProductId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Product> result = new HashMap<>();
        productMapper.selectBatchIds(ids).forEach(product -> result.put(product.getId(), product));
        return result;
    }

    private long previousSuccessfulOrderCount(Order order) {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, order.getUserId()).eq(Order::getMerchantId, order.getMerchantId())
                .ne(Order::getId, order.getId())
                .in(Order::getStatus, List.of(OrderStatus.WAIT_SHIP.getCode(), OrderStatus.WAIT_RECEIVE.getCode(),
                        OrderStatus.FINISHED.getCode(), OrderStatus.WAIT_GROUP.getCode(), OrderStatus.GROUP_SUCCESS.getCode()))
                .isNotNull(Order::getPayTime));
    }

    private boolean isActivityOrder(Order order) {
        return Integer.valueOf(1).equals(order.getOrderType()) || Integer.valueOf(2).equals(order.getOrderType());
    }

    private Set<Long> parseIds(String json) {
        if (json == null || json.isBlank()) return Set.of();
        String normalized = json.replaceAll("[\\[\\]\\s]", "");
        if (normalized.isBlank()) return Set.of();
        Set<Long> result = new HashSet<>();
        for (String value : normalized.split(",")) {
            try { result.add(Long.parseLong(value)); } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private String statusText(UserCoupon coupon) {
        if (coupon.getStatus() == UserCouponStatus.INVALID.getCode() && coupon.getInvalidReason() != null) return "已失效";
        if (coupon.getStatus() == UserCouponStatus.EXPIRED.getCode()
                || (coupon.getStatus() == UserCouponStatus.WAIT_USE.getCode() && coupon.getValidTo() != null
                && LocalDateTime.now().isAfter(coupon.getValidTo()))) return "已过期";
        return Arrays.stream(UserCouponStatus.values()).filter(item -> item.getCode() == coupon.getStatus())
                .map(UserCouponStatus::getText).findFirst().orElse("未知");
    }
}
