package com.shop.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.service.CouponService;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.dto.AddressSnapshot;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.points.dto.*;
import com.shop.points.entity.*;
import com.shop.points.mapper.*;
import com.shop.points.service.PointsMemberService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 积分账本是唯一余额来源；任何加减分均通过 appendLedger 落一条不可修改流水。 */
@Service
@RequiredArgsConstructor
public class PointsMemberServiceImpl implements PointsMemberService {
    private final MarketingFeatureService featureService;
    private final MemberProfileMapper profileMapper; private final PointsAccountMapper accountMapper;
    private final PointsLedgerMapper ledgerMapper; private final PointsRuleMapper ruleMapper;
    private final PointsProductMapper productMapper; private final PointsRedeemRecordMapper redeemMapper;
    private final MemberDayActivityMapper memberDayMapper; private final ProductMapper goodsMapper;
    private final ProductSkuMapper skuMapper; private final UserAddressMapper addressMapper;
    private final OrderMapper orderMapper; private final OrderItemMapper orderItemMapper;
    private final CouponService couponService; private final CouponTemplateMapper couponTemplateMapper; private final ObjectMapper objectMapper;

    @Override @Transactional
    public void registerMember(Long userId, Long merchantId) {
        if (featureService.isEnabled(merchantId, MarketingActivityCode.POINTS_MEMBER_DAY)) ensureMember(userId, merchantId);
    }

    @Override @Transactional
    public PointsProfileVO profile(Long userId, Long merchantId) {
        assertEnabled(merchantId); MemberProfile profile = ensureMember(userId, merchantId);
        PointsAccount account = ensureAccount(userId, merchantId);
        PointsProfileVO vo = new PointsProfileVO(); vo.setBalance(account.getBalance()); vo.setJoinedAt(profile.getJoinedAt());
        vo.setRedeemableCouponCount(productMapper.selectCount(new LambdaQueryWrapper<PointsProduct>()
                .eq(PointsProduct::getMerchantId, merchantId).isNotNull(PointsProduct::getCouponTemplateId).eq(PointsProduct::getStatus, 1)).intValue());
        MemberDayActivity activity = currentActivity(merchantId); vo.setMemberDayActive(activity != null); vo.setMemberDay(activity == null ? null : activity.getDayOfMonth()); return vo;
    }

    @Override public List<PointsLedgerVO> ledger(Long userId, Long merchantId, int limit) {
        assertEnabled(merchantId); return ledgerMapper.selectList(new LambdaQueryWrapper<PointsLedger>().eq(PointsLedger::getUserId, userId)
                .eq(PointsLedger::getMerchantId, merchantId).orderByDesc(PointsLedger::getId).last("LIMIT " + Math.min(Math.max(limit, 1), 100)))
                .stream().map(this::ledgerVO).toList();
    }

    @Override @Transactional
    public PointsProfileVO signIn(Long userId, Long merchantId) {
        assertEnabled(merchantId); PointsRule rule = activeRule(merchantId); String key = LocalDate.now() + "";
        appendLedger(userId, merchantId, safe(rule.getSignInPoints()), "SIGN_IN", key, null, "每日签到", rule.getValidDays());
        return profile(userId, merchantId);
    }

    @Override public List<PointsProductVO> mall(Long userId, Long merchantId) {
        assertEnabled(merchantId); LocalDateTime now = LocalDateTime.now();
        return productMapper.selectList(new LambdaQueryWrapper<PointsProduct>().eq(PointsProduct::getMerchantId, merchantId).eq(PointsProduct::getStatus, 1)
                .and(q -> q.isNull(PointsProduct::getValidFrom).or().le(PointsProduct::getValidFrom, now))
                .and(q -> q.isNull(PointsProduct::getValidTo).or().ge(PointsProduct::getValidTo, now)).orderByDesc(PointsProduct::getId))
                .stream().map(p -> productVO(p, userId, merchantId)).toList();
    }

    @Override @Transactional
    public PointsRedeemVO redeem(Long userId, Long merchantId, PointsRedeemRequest req) {
        assertEnabled(merchantId); PointsProduct product = productMapper.selectById(req.getPointsProductId()); LocalDateTime now = LocalDateTime.now();
        if (product == null || !merchantId.equals(product.getMerchantId()) || !Integer.valueOf(1).equals(product.getStatus()) || !available(product, now)) throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_FOUND);
        int quantity = req.getQuantity(); int limit = safe(product.getPerUserLimit());
        int redeemedQuantity = redeemMapper.selectList(new LambdaQueryWrapper<PointsRedeemRecord>().eq(PointsRedeemRecord::getUserId, userId)
                .eq(PointsRedeemRecord::getMerchantId, merchantId).eq(PointsRedeemRecord::getPointsProductId, product.getId()).eq(PointsRedeemRecord::getStatus, 1))
                .stream().mapToInt(item -> safe(item.getQuantity())).sum();
        if (limit > 0 && redeemedQuantity + quantity > limit) throw new BusinessException(ErrorCode.POINTS_REDEEM_LIMIT);
        if (productMapper.deductStock(product.getId(), quantity) == 0) throw new BusinessException(ErrorCode.POINTS_PRODUCT_SOLD_OUT);
        String redeemNo = "PT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        int cost = Math.multiplyExact(product.getPointsPrice(), quantity); PointsLedger ledger = appendLedger(userId, merchantId, -cost, "REDEEM", redeemNo, null, "兑换：" + product.getTitle(), 0);
        PointsRedeemRecord record = new PointsRedeemRecord(); record.setRedeemNo(redeemNo); record.setUserId(userId); record.setMerchantId(merchantId); record.setPointsProductId(product.getId()); record.setPointsCost(cost); record.setQuantity(quantity); record.setStatus(1);
        if (product.getCouponTemplateId() != null) { record.setCouponId(couponService.issueTemplate(userId, merchantId, product.getCouponTemplateId())); redeemMapper.insert(record); return redeemVO(record); }
        if (product.getProductId() == null || product.getSkuId() == null || req.getAddressId() == null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        Product goods = goodsMapper.selectById(product.getProductId()); ProductSku sku = skuMapper.selectById(product.getSkuId()); UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getId, req.getAddressId()).eq(UserAddress::getUserId, userId));
        if (goods == null || sku == null || address == null || !merchantId.equals(goods.getMerchantId()) || !Integer.valueOf(1).equals(goods.getStatus()) || !Integer.valueOf(1).equals(sku.getActive())) throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_FOUND);
        if (skuMapper.deductStock(sku.getId(), quantity) == 0) throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        redeemMapper.insert(record); String orderNo = "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Order order = new Order(); order.setOrderNo(orderNo); order.setUserId(userId); order.setMerchantId(merchantId); order.setOrderType(3); order.setPointsRedeemId(record.getId()); order.setStatus(OrderStatus.WAIT_SHIP.getCode()); order.setTotalAmount(BigDecimal.ZERO); order.setFreightAmount(BigDecimal.ZERO); order.setDiscountAmount(BigDecimal.ZERO); order.setPayAmount(BigDecimal.ZERO); order.setPayMethod(3); order.setPayTime(now); order.setRemark("积分兑换，商家包邮");
        try { order.setAddressSnapshot(objectMapper.writeValueAsString(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail()))); } catch (Exception e) { throw new IllegalStateException("地址快照失败", e); }
        orderMapper.insert(order); OrderItem item = new OrderItem(); item.setOrderId(order.getId()); item.setOrderNo(orderNo); item.setProductId(goods.getId()); item.setSkuId(sku.getId()); item.setProductName(goods.getName()); item.setMainImage(goods.getMainImage()); item.setSpecText(sku.getSpecText()); item.setUnitPrice(BigDecimal.ZERO); item.setQuantity(quantity); item.setSubtotal(BigDecimal.ZERO); orderItemMapper.insert(item);
        record.setOrderNo(orderNo); redeemMapper.updateById(record); return redeemVO(record);
    }

    @Override public MemberDayActivityVO memberDay(Long merchantId) { assertEnabled(merchantId); return activityVO(latestActivity(merchantId), currentActivity(merchantId) != null); }
    @Override @Transactional public Long receiveMemberDayCoupon(Long userId, Long merchantId) {
        assertEnabled(merchantId); MemberDayActivity activity = currentActivity(merchantId);
        if (activity == null || activity.getCouponTemplateId() == null) throw new BusinessException(ErrorCode.MEMBER_DAY_INACTIVE);
        String businessNo = "MEMBER_DAY_COUPON:" + activity.getId() + ":" + LocalDate.now();
        PointsLedger existing = ledgerMapper.selectOne(new LambdaQueryWrapper<PointsLedger>().eq(PointsLedger::getUserId, userId).eq(PointsLedger::getMerchantId, merchantId).eq(PointsLedger::getSource, "MEMBER_DAY_COUPON").eq(PointsLedger::getBusinessNo, businessNo));
        if (existing != null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "今日已领取会员券");
        appendLedger(userId, merchantId, 0, "MEMBER_DAY_COUPON", businessNo, null, "领取会员日专享券", 0);
        return couponService.issueTemplate(userId, merchantId, activity.getCouponTemplateId());
    }
    @Override public PointsRuleRequest rule(Long merchantId) { PointsRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<PointsRule>().eq(PointsRule::getMerchantId, merchantId)); return rule == null ? new PointsRuleRequest() : ruleVO(rule); }
    @Override @Transactional public void saveRule(Long merchantId, PointsRuleRequest req) { PointsRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<PointsRule>().eq(PointsRule::getMerchantId, merchantId)); if (rule == null) { rule = new PointsRule(); rule.setMerchantId(merchantId); } rule.setRegisterPoints(safe(req.getRegisterPoints())); rule.setPayAmountYuan(req.getPayAmountYuan()); rule.setPointsPerYuan(safe(req.getPointsPerYuan())); rule.setSignInPoints(safe(req.getSignInPoints())); rule.setValidDays(safe(req.getValidDays())); rule.setDeductionPerYuan(req.getDeductionPerYuan()); rule.setDeductionMaxPoints(safe(req.getDeductionMaxPoints())); rule.setStatus(req.getStatus()); if (rule.getId() == null) ruleMapper.insert(rule); else ruleMapper.updateById(rule); }
    @Override public List<PointsProductVO> merchantProducts(Long merchantId) { return productMapper.selectList(new LambdaQueryWrapper<PointsProduct>().eq(PointsProduct::getMerchantId, merchantId).orderByDesc(PointsProduct::getId)).stream().map(p -> productVO(p, null, merchantId)).toList(); }
    @Override @Transactional public Long saveProduct(Long merchantId, Long id, PointsProductRequest req) { PointsProduct product = id == null ? new PointsProduct() : productMapper.selectById(id); if (product == null || (product.getId() != null && !merchantId.equals(product.getMerchantId()))) throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_FOUND); if ((req.getCouponTemplateId() == null) == (req.getProductId() == null || req.getSkuId() == null)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "兑换项必须是优惠券或完整的实物SKU"); if (id == null) product.setMerchantId(merchantId); product.setProductId(req.getProductId()); product.setSkuId(req.getSkuId()); product.setCouponTemplateId(req.getCouponTemplateId()); product.setTitle(req.getTitle()); product.setImage(resolveExchangeImage(req)); product.setPointsPrice(req.getPointsPrice()); product.setStock(req.getStock()); product.setPerUserLimit(safe(req.getPerUserLimit())); product.setValidFrom(req.getValidFrom()); product.setValidTo(req.getValidTo()); product.setStatus(req.getStatus()); if (id == null) { productMapper.insert(product); return product.getId(); } productMapper.updateById(product); return product.getId(); }
    @Override @Transactional public void deleteProduct(Long merchantId, Long id) { PointsProduct product=productMapper.selectById(id); if(product==null||!merchantId.equals(product.getMerchantId()))throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_FOUND); if(Integer.valueOf(1).equals(product.getStatus()))throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),"请先下架兑换项后再删除"); productMapper.deleteById(id); }
    @Override public MemberDayActivityVO merchantMemberDay(Long merchantId) { return activityVO(latestActivity(merchantId), currentActivity(merchantId) != null); }
    @Override @Transactional public void saveMemberDay(Long merchantId, MemberDayActivityRequest req) { if (!req.getEndTime().isAfter(req.getStartTime())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "结束时间必须晚于开始时间"); MemberDayActivity activity = latestActivity(merchantId); if (activity == null) { activity = new MemberDayActivity(); activity.setMerchantId(merchantId); } activity.setName(req.getName()); activity.setDayOfMonth(req.getDayOfMonth()); activity.setStartTime(req.getStartTime()); activity.setEndTime(req.getEndTime()); activity.setDoublePoints(req.getDoublePoints()); activity.setCouponTemplateId(req.getCouponTemplateId()); activity.setProductScopeType(req.getProductScopeType()); activity.setProductScopeIdsJson(req.getProductScopeIdsJson()); activity.setStackable(req.getStackable()); activity.setStatus(req.getStatus()); if (activity.getId() == null) memberDayMapper.insert(activity); else memberDayMapper.updateById(activity); }

    @Override @Transactional public void rewardPaidOrder(Order order) { if (order == null || order.getOrderType() != null && order.getOrderType() == 3 || !featureService.isEnabled(order.getMerchantId(), MarketingActivityCode.POINTS_MEMBER_DAY)) return; PointsRule rule = activeRuleOrNull(order.getMerchantId()); if (rule == null) return; BigDecimal base = Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO).subtract(Optional.ofNullable(order.getDiscountAmount()).orElse(BigDecimal.ZERO)).max(BigDecimal.ZERO); int threshold = Math.max(1, safe(rule.getPayAmountYuan())); int points = base.setScale(0, RoundingMode.DOWN).intValue() / threshold * safe(rule.getPointsPerYuan()); MemberDayActivity activity = currentActivity(order.getMerchantId()); if (activity != null && appliesToOrder(activity, order)) points *= 2; if (points > 0) appendLedger(order.getUserId(), order.getMerchantId(), points, "ORDER_PAY", order.getOrderNo(), null, "订单支付赠分", rule.getValidDays()); }
    @Override @Transactional public void reverseRefund(Long merchantId, Long userId, String refundNo, BigDecimal amount, String orderNo) { PointsLedger original = ledgerMapper.selectOne(new LambdaQueryWrapper<PointsLedger>().eq(PointsLedger::getUserId, userId).eq(PointsLedger::getMerchantId, merchantId).eq(PointsLedger::getSource, "ORDER_PAY").eq(PointsLedger::getBusinessNo, orderNo)); if (original == null || amount == null || amount.signum() <= 0) return; Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)); BigDecimal paid = order == null ? null : order.getPayAmount(); if (paid == null || paid.signum() <= 0) return; int points = BigDecimal.valueOf(safe(original.getChangeValue())).multiply(amount).divide(paid, 0, RoundingMode.DOWN).intValue(); if (points > 0) appendLedger(userId, merchantId, -points, "REFUND", refundNo, original.getId(), "订单退款积分冲正", 0); }

    private void assertEnabled(Long merchantId) { featureService.assertEnabled(merchantId, MarketingActivityCode.POINTS_MEMBER_DAY); }
    private MemberProfile ensureMember(Long userId, Long merchantId) { MemberProfile p = profileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId).eq(MemberProfile::getMerchantId, merchantId)); if (p != null) return p; p = new MemberProfile(); p.setUserId(userId); p.setMerchantId(merchantId); p.setStatus(1); p.setJoinedAt(LocalDateTime.now()); profileMapper.insert(p); PointsRule rule = activeRuleOrNull(merchantId); if (rule != null && safe(rule.getRegisterPoints()) > 0) appendLedger(userId, merchantId, rule.getRegisterPoints(), "REGISTER", "MEMBER:" + userId, null, "注册会员赠分", rule.getValidDays()); return p; }
    private PointsAccount ensureAccount(Long userId, Long merchantId) { PointsAccount a = accountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>().eq(PointsAccount::getUserId, userId).eq(PointsAccount::getMerchantId, merchantId)); if (a != null) return a; a = new PointsAccount(); a.setUserId(userId); a.setMerchantId(merchantId); a.setBalance(0); a.setVersion(0); accountMapper.insert(a); return a; }
    private PointsLedger appendLedger(Long userId, Long merchantId, int delta, String source, String businessNo, Long relatedId, String description, Integer validDays) { PointsLedger existing = ledgerMapper.selectOne(new LambdaQueryWrapper<PointsLedger>().eq(PointsLedger::getUserId, userId).eq(PointsLedger::getMerchantId, merchantId).eq(PointsLedger::getSource, source).eq(PointsLedger::getBusinessNo, businessNo)); if (existing != null) return existing; ensureMemberWithoutReward(userId, merchantId); PointsAccount account = accountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>().eq(PointsAccount::getUserId, userId).eq(PointsAccount::getMerchantId, merchantId).last("FOR UPDATE")); if (account == null) { ensureAccount(userId, merchantId); account = accountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>().eq(PointsAccount::getUserId, userId).eq(PointsAccount::getMerchantId, merchantId).last("FOR UPDATE")); } int after = safe(account.getBalance()) + delta; if (after < 0) throw new BusinessException(ErrorCode.POINTS_NOT_ENOUGH); account.setBalance(after); account.setVersion(safe(account.getVersion()) + 1); accountMapper.updateById(account); PointsLedger ledger = new PointsLedger(); ledger.setUserId(userId); ledger.setMerchantId(merchantId); ledger.setChangeValue(delta); ledger.setBalanceAfter(after); ledger.setSource(source); ledger.setBusinessNo(businessNo); ledger.setRelatedLedgerId(relatedId); ledger.setDescription(description); ledger.setExpireAt(validDays != null && validDays > 0 && delta > 0 ? LocalDateTime.now().plusDays(validDays) : null); ledgerMapper.insert(ledger); return ledger; }
    private void ensureMemberWithoutReward(Long userId, Long merchantId) { if (profileMapper.selectCount(new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId,userId).eq(MemberProfile::getMerchantId,merchantId)) == 0) { MemberProfile p=new MemberProfile();p.setUserId(userId);p.setMerchantId(merchantId);p.setStatus(1);p.setJoinedAt(LocalDateTime.now());profileMapper.insert(p); } }
    private PointsRule activeRule(Long merchantId) { PointsRule r = activeRuleOrNull(merchantId); if (r == null) throw new BusinessException(ErrorCode.POINTS_RULE_UNPUBLISHED); return r; }
    private PointsRule activeRuleOrNull(Long merchantId) { return ruleMapper.selectOne(new LambdaQueryWrapper<PointsRule>().eq(PointsRule::getMerchantId, merchantId).eq(PointsRule::getStatus, 1)); }
    private MemberDayActivity latestActivity(Long merchantId) { return memberDayMapper.selectOne(new LambdaQueryWrapper<MemberDayActivity>().eq(MemberDayActivity::getMerchantId, merchantId).orderByDesc(MemberDayActivity::getId)); }
    private MemberDayActivity currentActivity(Long merchantId) { MemberDayActivity a = latestActivity(merchantId); if (a == null || !Integer.valueOf(1).equals(a.getStatus())) return null; LocalDateTime now=LocalDateTime.now(); if (now.getDayOfMonth()!=a.getDayOfMonth() || now.toLocalTime().isBefore(a.getStartTime()) || now.toLocalTime().isAfter(a.getEndTime())) return null; return a; }
    private boolean available(PointsProduct p, LocalDateTime now) { return (p.getValidFrom()==null || !now.isBefore(p.getValidFrom())) && (p.getValidTo()==null || !now.isAfter(p.getValidTo())); }
    private boolean appliesToOrder(MemberDayActivity activity, Order order) {
        if (safe(activity.getProductScopeType()) == 0 || activity.getProductScopeIdsJson() == null || activity.getProductScopeIdsJson().isBlank()) return true;
        try {
            Set<Long> scopes = new HashSet<>(objectMapper.readValue(activity.getProductScopeIdsJson(), new TypeReference<List<Long>>() {}));
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            if (safe(activity.getProductScopeType()) == 1) return items.stream().anyMatch(item -> scopes.contains(item.getProductId()));
            if (safe(activity.getProductScopeType()) == 2) return items.stream().map(item -> goodsMapper.selectById(item.getProductId())).filter(Objects::nonNull).anyMatch(goods -> scopes.contains(goods.getCategoryId()));
            return false;
        } catch (Exception ignored) { return false; }
    }
    private int safe(Integer n) { return n == null ? 0 : n; }
    private PointsLedgerVO ledgerVO(PointsLedger x){ PointsLedgerVO v=new PointsLedgerVO();v.setId(x.getId());v.setChangeValue(x.getChangeValue());v.setBalanceAfter(x.getBalanceAfter());v.setSource(x.getSource());v.setDescription(x.getDescription());v.setBusinessNo(x.getBusinessNo());v.setCreatedAt(x.getCreatedAt());return v; }
    private PointsProductVO productVO(PointsProduct p, Long uid, Long mid){ PointsProductVO v=new PointsProductVO();v.setId(p.getId());v.setProductId(p.getProductId());v.setSkuId(p.getSkuId());v.setCouponTemplateId(p.getCouponTemplateId());v.setTitle(p.getTitle());v.setImage(displayImage(p));v.setPointsPrice(p.getPointsPrice());v.setStock(p.getStock());v.setPerUserLimit(p.getPerUserLimit());v.setStatus(p.getStatus());v.setPhysical(p.getProductId()!=null); if(uid!=null) v.setRedeemedCount(Math.toIntExact(redeemMapper.selectCount(new LambdaQueryWrapper<PointsRedeemRecord>().eq(PointsRedeemRecord::getUserId,uid).eq(PointsRedeemRecord::getMerchantId,mid).eq(PointsRedeemRecord::getPointsProductId,p.getId()).eq(PointsRedeemRecord::getStatus,1))));return v; }
    private String resolveExchangeImage(PointsProductRequest req){ if(req.getProductId()!=null){ ProductSku sku=req.getSkuId()==null?null:skuMapper.selectById(req.getSkuId()); if(sku!=null&&sku.getImage()!=null&&!sku.getImage().isBlank())return sku.getImage(); Product goods=goodsMapper.selectById(req.getProductId()); if(goods!=null&&goods.getMainImage()!=null&&!goods.getMainImage().isBlank())return goods.getMainImage(); } if(req.getCouponTemplateId()!=null){ CouponTemplate template=couponTemplateMapper.selectById(req.getCouponTemplateId()); if(template!=null&&template.getImage()!=null&&!template.getImage().isBlank())return template.getImage(); } return req.getImage()==null?"":req.getImage(); }
    private String displayImage(PointsProduct p){ if(p.getProductId()!=null){ ProductSku sku=p.getSkuId()==null?null:skuMapper.selectById(p.getSkuId()); if(sku!=null&&sku.getImage()!=null&&!sku.getImage().isBlank())return sku.getImage(); Product goods=goodsMapper.selectById(p.getProductId()); if(goods!=null&&goods.getMainImage()!=null&&!goods.getMainImage().isBlank())return goods.getMainImage(); } if(p.getCouponTemplateId()!=null){ CouponTemplate template=couponTemplateMapper.selectById(p.getCouponTemplateId()); if(template!=null&&template.getImage()!=null&&!template.getImage().isBlank())return template.getImage(); } return p.getImage()==null?"":p.getImage(); }
    private PointsRuleRequest ruleVO(PointsRule r){ PointsRuleRequest v=new PointsRuleRequest();v.setRegisterPoints(r.getRegisterPoints());v.setPayAmountYuan(r.getPayAmountYuan() == null ? 1 : r.getPayAmountYuan());v.setPointsPerYuan(r.getPointsPerYuan());v.setSignInPoints(r.getSignInPoints());v.setValidDays(r.getValidDays());v.setDeductionPerYuan(r.getDeductionPerYuan());v.setDeductionMaxPoints(r.getDeductionMaxPoints());v.setStatus(r.getStatus());return v; }
    private MemberDayActivityVO activityVO(MemberDayActivity a, boolean active){ if(a==null)return null;MemberDayActivityVO v=new MemberDayActivityVO();v.setId(a.getId());v.setName(a.getName());v.setDayOfMonth(a.getDayOfMonth());v.setStartTime(a.getStartTime());v.setEndTime(a.getEndTime());v.setDoublePoints(a.getDoublePoints());v.setCouponTemplateId(a.getCouponTemplateId());v.setProductScopeType(a.getProductScopeType());v.setProductScopeIdsJson(a.getProductScopeIdsJson());v.setStackable(a.getStackable());v.setStatus(a.getStatus());v.setActive(active);v.setStatusText(active?"进行中":Integer.valueOf(1).equals(a.getStatus())?"未开始或已结束":"未发布");return v; }
    private PointsRedeemVO redeemVO(PointsRedeemRecord r){PointsRedeemVO v=new PointsRedeemVO();v.setRedeemNo(r.getRedeemNo());v.setOrderNo(r.getOrderNo());v.setCouponId(r.getCouponId());v.setPointsCost(r.getPointsCost());return v;}
}
