package com.shop.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.coupon.dto.*;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.entity.UserCoupon;
import com.shop.coupon.enums.CouponTemplateStatus;
import com.shop.coupon.enums.CouponTemplateType;
import com.shop.coupon.enums.UserCouponStatus;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.coupon.service.CouponService;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private static final int SCOPE_ALL = 0;
    private static final int SCOPE_CATEGORY = 1;
    private static final int SCOPE_PRODUCT = 2;

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderMapper orderMapper;
    private final MarketingFeatureService marketingFeatureService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void initializeMerchant(Long merchantId) {
        if (merchantId == null || templateMapper.selectCount(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getMerchantId, merchantId)) > 0) return;
        CouponTemplateSaveRequest request = new CouponTemplateSaveRequest();
        request.setName("新人首单券"); request.setAmount(new BigDecimal("20.00"));
        request.setThresholdAmount(new BigDecimal("99.00")); request.setTotalStock(0);
        request.setPerUserLimit(1); request.setValidityDays(30); request.setScopeType(SCOPE_ALL);
        request.setExcludeActivityGoods(1); request.setStackable(0); request.setStatus(CouponTemplateStatus.ACTIVE.getCode());
        createTemplate(merchantId, request);
    }

    @Override
    public List<CouponTemplateVO> listTemplates(Long merchantId) {
        return templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getMerchantId, merchantId)
                        .orderByDesc(CouponTemplate::getId))
                .stream().map(this::toTemplateVO).toList();
    }

    @Override
    @Transactional
    public Long createTemplate(Long merchantId, CouponTemplateSaveRequest request) {
        validateRequest(request);
        CouponTemplate template = new CouponTemplate();
        applyRequest(template, request);
        template.setMerchantId(merchantId);
        template.setType(CouponTemplateType.FULL_REDUCTION.getCode());
        template.setReceivedCount(0);
        template.setUsedCount(0);
        template.setNewUserOnly(1);
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional
    public void updateTemplate(Long merchantId, Long templateId, CouponTemplateSaveRequest request) {
        validateRequest(request);
        CouponTemplate template = ownedTemplate(merchantId, templateId);
        applyRequest(template, request);
        // 已发放的 UserCoupon 使用快照，修改模板不会影响历史券。
        templateMapper.updateById(template);
    }

    @Override
    @Transactional
    public void updateTemplateStatus(Long merchantId, Long templateId, Integer status) {
        if (status == null || (status != CouponTemplateStatus.DRAFT.getCode()
                && status != CouponTemplateStatus.ACTIVE.getCode()
                && status != CouponTemplateStatus.STOPPED.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        CouponTemplate template = ownedTemplate(merchantId, templateId);
        template.setStatus(status);
        templateMapper.updateById(template);
    }

    @Override
    public NewUserCouponEligibilityVO eligibility(Long userId, Long merchantId) {
        NewUserCouponEligibilityVO vo = new NewUserCouponEligibilityVO();
        vo.setCanReceive(false);
        vo.setReceived(false);
        if (!marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.NEW_USER_COUPON)) {
            return vo;
        }
        UserCoupon issued = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getMerchantId, merchantId)
                .orderByDesc(UserCoupon::getId).last("LIMIT 1"));
        if (issued != null) {
            vo.setReceived(true);
            vo.setCoupon(toCouponVO(issued, LocalDateTime.now(), null));
            return vo;
        }
        if (hasSuccessfulOrder(userId, merchantId)) {
            return vo;
        }
        CouponTemplate template = activeNewUserTemplate(merchantId, LocalDateTime.now());
        if (template != null) {
            vo.setCanReceive(true);
            vo.setCoupon(toCouponVO(template));
        }
        return vo;
    }

    @Override
    @Transactional
    public Long receiveNewUserCoupon(Long userId, Long merchantId, Long templateId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.NEW_USER_COUPON);
        if (hasSuccessfulOrder(userId, merchantId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前用户不符合新人资格");
        }
        return issueTemplateInternal(userId, merchantId, templateId, true);
    }

    @Override
    @Transactional
    public Long issueTemplate(Long userId, Long merchantId, Long templateId) {
        return issueTemplateInternal(userId, merchantId, templateId, false);
    }

    @Override
    @Transactional
    public boolean invalidateCoupon(Long userId, Long merchantId, Long couponId) {
        if (userId == null || merchantId == null || couponId == null) return false;
        UserCoupon coupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getMerchantId, merchantId)
                .last("FOR UPDATE"));
        if (coupon == null || coupon.getStatus() != UserCouponStatus.WAIT_USE.getCode()) return false;
        coupon.setStatus(UserCouponStatus.INVALID.getCode());
        userCouponMapper.updateById(coupon);
        return true;
    }

    private Long issueTemplateInternal(Long userId, Long merchantId, Long templateId, boolean requireNewUser) {
        LocalDateTime now = LocalDateTime.now();
        CouponTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .eq(CouponTemplate::getMerchantId, merchantId)
                .last("FOR UPDATE"));
        if (template == null || !isActive(template, now)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "优惠券已停止发放");
        }
        if (requireNewUser && !Integer.valueOf(1).equals(template.getNewUserOnly())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前优惠券不是新人券");
        }
        if (requireNewUser && hasSuccessfulOrder(userId, merchantId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前用户不符合新人资格");
        }
        long existing = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId));
        if (existing >= Math.max(1, Optional.ofNullable(template.getPerUserLimit()).orElse(1))) {
            UserCoupon issued = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getTemplateId, templateId)
                    .orderByDesc(UserCoupon::getId).last("LIMIT 1"));
            return issued == null ? null : issued.getId();
        }
        if (template.getTotalStock() != null && template.getTotalStock() > 0
                && Optional.ofNullable(template.getReceivedCount()).orElse(0) >= template.getTotalStock()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "新人券已领完");
        }
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setMerchantId(merchantId);
        coupon.setTemplateId(template.getId());
        coupon.setTemplateNameSnapshot(template.getName());
        coupon.setType(template.getType());
        coupon.setAmountSnapshot(template.getAmount());
        coupon.setThresholdSnapshot(template.getThresholdAmount());
        coupon.setScopeTypeSnapshot(template.getScopeType());
        coupon.setScopeIdsSnapshot(template.getScopeIdsJson());
        coupon.setExcludeActivityGoodsSnapshot(template.getExcludeActivityGoods());
        coupon.setValidFrom(now);
        LocalDateTime validTo = now.plusDays(Math.max(1, template.getValidityDays()));
        if (template.getValidTo() != null && template.getValidTo().isBefore(validTo)) validTo = template.getValidTo();
        coupon.setValidTo(validTo);
        coupon.setStatus(UserCouponStatus.WAIT_USE.getCode());
        coupon.setReceivedAt(now);
        userCouponMapper.insert(coupon);
        template.setReceivedCount(Optional.ofNullable(template.getReceivedCount()).orElse(0) + 1);
        templateMapper.updateById(template);
        log.info("coupon issued userId={}, merchantId={}, couponId={}, requireNewUser={}", userId, merchantId, coupon.getId(), requireNewUser);
        return coupon.getId();
    }

    @Override
    public List<CouponVO> listUserCoupons(Long userId, Long merchantId, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getMerchantId, merchantId)
                        .orderByDesc(UserCoupon::getId))
                .stream().map(c -> toCouponVO(c, now, null))
                .filter(c -> status == null || Objects.equals(c.getStatus(), status)).toList();
    }

    @Override
    @Transactional
    public CouponCheckoutResult calculate(Long userId, CouponUseContext context, Long requestedCouponId,
                                          boolean consume, String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getMerchantId, context.getMerchantId())
                .orderByDesc(UserCoupon::getId));
        List<CouponVO> couponVOs = coupons.stream()
                .map(c -> toCouponVO(c, now, context)).toList();
        CouponCheckoutResult result = new CouponCheckoutResult();
        result.setCoupons(couponVOs);

        UserCoupon selected = null;
        if (requestedCouponId != null) {
            selected = coupons.stream().filter(c -> requestedCouponId.equals(c.getId())).findFirst().orElse(null);
            String reason = selected == null ? "优惠券不存在或不属于当前商家" : unavailableReason(selected, now, context);
            if (reason != null) {
                result.setUnavailableReason(reason);
                if (consume) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), reason);
                return result;
            }
        } else {
            selected = coupons.stream()
                    .filter(c -> unavailableReason(c, now, context) == null)
                    .max(Comparator.comparing(c -> discountFor(c, eligibleAmount(c, context.getItems()))))
                    .orElse(null);
        }
        if (selected == null) return result;
        if (consume) {
            selected = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, selected.getId())
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getMerchantId, context.getMerchantId())
                    .last("FOR UPDATE"));
            if (selected == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "优惠券不存在");
            String reason = unavailableReason(selected, now, context);
            if (reason != null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), reason);
            selected.setStatus(UserCouponStatus.USED.getCode());
            selected.setUsedAt(now);
            selected.setUsedOrderNo(orderNo);
            userCouponMapper.updateById(selected);
            CouponTemplate template = templateMapper.selectById(selected.getTemplateId());
            if (template != null) {
                template.setUsedCount(Optional.ofNullable(template.getUsedCount()).orElse(0) + 1);
                templateMapper.updateById(template);
            }
        }
        result.setSelectedCouponId(selected.getId());
        result.setSelectedCouponTemplateId(selected.getTemplateId());
        result.setSelectedCouponName(selected.getTemplateNameSnapshot());
        result.setDiscountAmount(discountFor(selected, eligibleAmount(selected, context.getItems())));
        return result;
    }

    @Override
    @Transactional
    public void releaseBeforePaymentCancel(Long orderId, String orderNo) {
        if (orderNo == null && orderId != null) {
            Order order = orderMapper.selectById(orderId);
            orderNo = order == null ? null : order.getOrderNo();
        }
        if (orderNo == null) return;
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getStatus, UserCouponStatus.USED.getCode());
        wrapper.eq(UserCoupon::getUsedOrderNo, orderNo);
        UserCoupon coupon = userCouponMapper.selectOne(wrapper.last("FOR UPDATE"));
        if (coupon == null) return;
        coupon.setStatus(UserCouponStatus.WAIT_USE.getCode());
        coupon.setUsedAt(null);
        coupon.setUsedOrderNo(null);
        userCouponMapper.updateById(coupon);
        CouponTemplate template = templateMapper.selectById(coupon.getTemplateId());
        if (template != null && Optional.ofNullable(template.getUsedCount()).orElse(0) > 0) {
            template.setUsedCount(template.getUsedCount() - 1);
            templateMapper.updateById(template);
        }
    }

    private boolean hasSuccessfulOrder(Long userId, Long merchantId) {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getMerchantId, merchantId)
                .in(Order::getStatus, List.of(OrderStatus.WAIT_SHIP.getCode(), OrderStatus.WAIT_RECEIVE.getCode(),
                        OrderStatus.FINISHED.getCode(), OrderStatus.WAIT_GROUP.getCode(), OrderStatus.GROUP_SUCCESS.getCode()))
                .isNotNull(Order::getPayTime)) > 0;
    }

    private CouponTemplate activeNewUserTemplate(Long merchantId, LocalDateTime now) {
        return templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getMerchantId, merchantId)
                        .eq(CouponTemplate::getStatus, CouponTemplateStatus.ACTIVE.getCode())
                        .eq(CouponTemplate::getNewUserOnly, 1)
                        .orderByDesc(CouponTemplate::getAmount))
                .stream().filter(t -> isActive(t, now)).findFirst().orElse(null);
    }

    private boolean isActive(CouponTemplate t, LocalDateTime now) {
        return t.getStatus() == CouponTemplateStatus.ACTIVE.getCode()
                && (t.getValidFrom() == null || !now.isBefore(t.getValidFrom()))
                && (t.getValidTo() == null || !now.isAfter(t.getValidTo()));
    }

    private String unavailableReason(UserCoupon coupon, LocalDateTime now, CouponUseContext context) {
        if (coupon.getStatus() != UserCouponStatus.WAIT_USE.getCode()) return statusText(coupon.getStatus());
        if (coupon.getValidTo() == null || now.isAfter(coupon.getValidTo())) return "优惠券已过期";
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) return "优惠券尚未生效";
        BigDecimal eligibleAmount = eligibleAmount(coupon, context.getItems());
        if (eligibleAmount.signum() <= 0) return "没有符合条件的商品";
        if (coupon.getThresholdSnapshot() != null && eligibleAmount.compareTo(coupon.getThresholdSnapshot()) < 0) {
            return "未满足满" + money(coupon.getThresholdSnapshot()) + "元使用门槛";
        }
        return null;
    }

    private BigDecimal eligibleAmount(UserCoupon coupon, List<CouponItemContext> items) {
        Set<Long> scopeIds = parseIds(coupon.getScopeIdsSnapshot());
        return (items == null ? List.<CouponItemContext>of() : items).stream()
                .filter(item -> !Integer.valueOf(1).equals(coupon.getExcludeActivityGoodsSnapshot()) || !item.isActivityGoods())
                .filter(item -> coupon.getScopeTypeSnapshot() == null || coupon.getScopeTypeSnapshot() == SCOPE_ALL
                        || (coupon.getScopeTypeSnapshot() == SCOPE_PRODUCT && scopeIds.contains(item.getProductId()))
                        || (coupon.getScopeTypeSnapshot() == SCOPE_CATEGORY && scopeIds.contains(item.getCategoryId())))
                .map(CouponItemContext::getSubtotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal discountFor(UserCoupon coupon, BigDecimal goodsAmount) {
        BigDecimal amount = Optional.ofNullable(coupon.getAmountSnapshot()).orElse(BigDecimal.ZERO);
        return amount.min(Optional.ofNullable(goodsAmount).orElse(BigDecimal.ZERO)).max(BigDecimal.ZERO);
    }

    private CouponVO toCouponVO(UserCoupon coupon, LocalDateTime now, CouponUseContext context) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId()); vo.setTemplateId(coupon.getTemplateId());
        vo.setName(coupon.getTemplateNameSnapshot()); vo.setType(coupon.getType());
        vo.setAmount(coupon.getAmountSnapshot()); vo.setThresholdAmount(coupon.getThresholdSnapshot());
        vo.setValidFrom(coupon.getValidFrom()); vo.setValidTo(coupon.getValidTo());
        Integer status = coupon.getStatus();
        if (status == UserCouponStatus.WAIT_USE.getCode() && coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            status = UserCouponStatus.EXPIRED.getCode();
        }
        vo.setStatus(status); vo.setStatusText(statusText(status));
        String reason = context == null ? (status == UserCouponStatus.WAIT_USE.getCode() ? null : statusText(status))
                : unavailableReason(coupon, now, context);
        vo.setAvailable(reason == null);
        vo.setUnavailableReason(reason);
        return vo;
    }

    private CouponVO toCouponVO(CouponTemplate template) {
        CouponVO vo = new CouponVO();
        vo.setTemplateId(template.getId()); vo.setName(template.getName()); vo.setType(template.getType());
        vo.setAmount(template.getAmount()); vo.setThresholdAmount(template.getThresholdAmount());
        vo.setValidFrom(template.getValidFrom()); vo.setValidTo(template.getValidTo());
        vo.setValidityDays(template.getValidityDays());
        vo.setAvailable(true); vo.setStatus(template.getStatus()); vo.setStatusText(CouponTemplateStatus.ACTIVE.getText());
        return vo;
    }

    private CouponTemplateVO toTemplateVO(CouponTemplate t) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(t.getId()); vo.setName(t.getName()); vo.setType(t.getType()); vo.setAmount(t.getAmount());
        vo.setThresholdAmount(t.getThresholdAmount()); vo.setTotalStock(t.getTotalStock());
        vo.setReceivedCount(t.getReceivedCount()); vo.setUsedCount(t.getUsedCount());
        vo.setPerUserLimit(t.getPerUserLimit()); vo.setValidityDays(t.getValidityDays());
        vo.setValidFrom(t.getValidFrom()); vo.setValidTo(t.getValidTo()); vo.setScopeType(t.getScopeType());
        vo.setScopeIds(new ArrayList<>(parseIds(t.getScopeIdsJson()))); vo.setNewUserOnly(t.getNewUserOnly());
        vo.setExcludeActivityGoods(t.getExcludeActivityGoods()); vo.setStackable(t.getStackable());
        vo.setStatus(t.getStatus()); vo.setStatusText(Arrays.stream(CouponTemplateStatus.values())
                .filter(s -> s.getCode() == t.getStatus()).map(CouponTemplateStatus::getText).findFirst().orElse("未知"));
        return vo;
    }

    private CouponTemplate ownedTemplate(Long merchantId, Long id) {
        CouponTemplate t = templateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, id).eq(CouponTemplate::getMerchantId, merchantId));
        if (t == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "优惠券模板不存在");
        return t;
    }

    private void validateRequest(CouponTemplateSaveRequest request) {
        if (request.getAmount().compareTo(request.getThresholdAmount()) > 0 && request.getThresholdAmount().signum() > 0) {
            // 面额大于门槛并非绝对错误，但会导致全额减免；保留为合法配置。
        }
        if (request.getPerUserLimit() > 1 || request.getNewUserOnly() == null || (request.getNewUserOnly() != 0 && request.getNewUserOnly() != 1)
                || request.getScopeType() == null || request.getScopeType() < 0 || request.getScopeType() > 2
                || request.getStatus() == null || request.getStatus() < 0 || request.getStatus() > 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "新人券每人限领1张，范围配置不合法");
        }
        if (request.getValidFrom() != null && request.getValidTo() != null && request.getValidFrom().isAfter(request.getValidTo())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "有效期起止时间不合法");
        }
    }

    private void applyRequest(CouponTemplate t, CouponTemplateSaveRequest request) {
        t.setName(request.getName().trim()); t.setAmount(request.getAmount()); t.setThresholdAmount(request.getThresholdAmount());
        t.setTotalStock(request.getTotalStock()); t.setPerUserLimit(1); t.setValidityDays(request.getValidityDays());
        t.setValidFrom(request.getValidFrom()); t.setValidTo(request.getValidTo()); t.setScopeType(request.getScopeType());
        t.setScopeIdsJson(toJson(request.getScopeIds() == null ? List.of() : request.getScopeIds()));
        t.setNewUserOnly(request.getNewUserOnly() == null ? 1 : request.getNewUserOnly());
        t.setExcludeActivityGoods(request.getExcludeActivityGoods() == null ? 1 : request.getExcludeActivityGoods());
        t.setStackable(0); t.setStatus(request.getStatus() == null ? CouponTemplateStatus.ACTIVE.getCode() : request.getStatus());
    }

    private Set<Long> parseIds(String json) {
        if (json == null || json.isBlank()) return Set.of();
        try { return new HashSet<>(objectMapper.readValue(json, new TypeReference<List<Long>>() {})); }
        catch (Exception ignored) { return Set.of(); }
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { return "[]"; }
    }

    private String statusText(Integer status) {
        return Arrays.stream(UserCouponStatus.values()).filter(s -> s.getCode() == status)
                .map(UserCouponStatus::getText).findFirst().orElse("未知");
    }

    private String money(BigDecimal amount) { return amount.stripTrailingZeros().toPlainString(); }
}
