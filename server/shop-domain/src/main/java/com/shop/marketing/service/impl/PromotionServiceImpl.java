package com.shop.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.marketing.dto.*;
import com.shop.marketing.entity.*;
import com.shop.marketing.mapper.*;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.marketing.service.PromotionService;
import com.shop.marketing.enums.MarketingActivityCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {
    private static final int SCOPE_CATEGORY = 1, SCOPE_PRODUCT = 2, SCOPE_EXCLUDED_PRODUCT = 3, SCOPE_RECOMMEND_PRODUCT = 4;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final PromotionActivityMapper activityMapper;
    private final PromotionThresholdMapper thresholdMapper;
    private final PromotionScopeMapper scopeMapper;
    private final PromotionOrderReservationMapper reservationMapper;
    private final MarketingFeatureService marketingFeatureService;

    @Override public List<PromotionActivityVO> list(Long merchantId) {
        return activityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>().eq(PromotionActivity::getMerchantId, merchantId)
                .orderByDesc(PromotionActivity::getPriority).orderByDesc(PromotionActivity::getId)).stream().map(this::toVO).toList();
    }
    @Override public List<PromotionActivityVO> listActive(Long merchantId) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        return activityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>().eq(PromotionActivity::getMerchantId, merchantId)
                .eq(PromotionActivity::getStatus, 1).le(PromotionActivity::getStartAt, now).gt(PromotionActivity::getEndAt, now)
                .orderByDesc(PromotionActivity::getPriority).orderByDesc(PromotionActivity::getId)).stream().map(this::toVO).toList();
    }
    @Override public PromotionActivityVO get(Long merchantId, Long id) { return toVO(requireOwned(merchantId, id)); }

    @Override @Transactional public Long create(Long merchantId, PromotionActivityRequest request) {
        validate(request);
        PromotionActivity entity = new PromotionActivity();
        entity.setMerchantId(merchantId); copy(request, entity); entity.setReservedBudget(BigDecimal.ZERO); entity.setPaidBudget(BigDecimal.ZERO);
        entity.setReservedOrderCount(0); entity.setPaidOrderCount(0); activityMapper.insert(entity); replaceChildren(entity.getId(), request); return entity.getId();
    }
    @Override @Transactional public void update(Long merchantId, Long id, PromotionActivityRequest request) {
        validate(request); PromotionActivity entity = requireOwned(merchantId, id); copy(request, entity); activityMapper.updateById(entity); replaceChildren(id, request);
    }
    @Override @Transactional public void updateStatus(Long merchantId, Long id, Integer status) {
        if (status == null || status < 0 || status > 2) throw new BusinessException(ErrorCode.PARAM_ERROR);
        PromotionActivity entity = requireOwned(merchantId, id);
        if (status == 1 && thresholdMapper.selectCount(new LambdaQueryWrapper<PromotionThreshold>().eq(PromotionThreshold::getActivityId, id)) == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "请先配置至少一个优惠阶梯再启用活动");
        }
        entity.setStatus(status); activityMapper.updateById(entity);
    }

    @Override public PromotionCheckoutResult calculate(Long merchantId, List<PromotionPricingItem> items) {
        PromotionCheckoutResult result = new PromotionCheckoutResult();
        if (!marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.FULL_REDUCTION) || items == null || items.isEmpty()) return result;
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        List<PromotionActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getMerchantId, merchantId).eq(PromotionActivity::getStatus, 1)
                .le(PromotionActivity::getStartAt, now).gt(PromotionActivity::getEndAt, now)
                .orderByDesc(PromotionActivity::getPriority).orderByDesc(PromotionActivity::getId));
        List<PromotionCheckoutResult.PromotionProgress> progresses = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();
        Candidate presentation = null;
        for (PromotionActivity activity : activities) {
            Candidate candidate = candidate(activity, items);
            if (candidate.threshold == null) continue;
            if (presentation == null) presentation = candidate;
            progresses.add(candidate.progress); if (candidate.discount.compareTo(BigDecimal.ZERO) > 0) candidates.add(candidate);
        }
        result.setProgresses(progresses);
        Candidate selected = candidates.isEmpty() ? presentation : candidates.get(0); // query order enforces merchant configured priority.
        if (selected == null || selected.threshold == null) return result;
        result.setActivityId(selected.activity.getId()); result.setActivityName(selected.activity.getName()); result.setActivityType(selected.activity.getActivityType());
        result.setQualifiedAmount(selected.qualified); result.setThresholdAmount(selected.threshold.getThresholdAmount()); result.setDiscountAmount(selected.discount);
        result.setNextThresholdAmount(selected.progress.getNextThresholdAmount()); result.setRemainingAmount(selected.progress.getRemainingAmount());
        result.setCouponStackable(selected.discount.compareTo(BigDecimal.ZERO) <= 0 || Integer.valueOf(1).equals(selected.activity.getStackNewUserCoupon()) || Integer.valueOf(1).equals(selected.activity.getStackRepurchaseCoupon()));
        result.setRecommendProductIds(Integer.valueOf(1).equals(selected.activity.getShowRecommendations())
                ? new ArrayList<>(scopeIds(selected.activity.getId(), SCOPE_RECOMMEND_PRODUCT)) : List.of());
        return result;
    }
    @Override public PromotionCheckoutResult calculateActivity(Long activityId, List<PromotionPricingItem> items) {
        PromotionCheckoutResult result = new PromotionCheckoutResult();
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null || items == null || items.isEmpty()) return result;
        Candidate selected = candidate(activity, items);
        result.setActivityId(activity.getId()); result.setActivityName(activity.getName()); result.setActivityType(activity.getActivityType());
        result.setQualifiedAmount(selected.qualified); result.setDiscountAmount(selected.discount); result.setThresholdAmount(selected.threshold == null ? null : selected.threshold.getThresholdAmount());
        result.setNextThresholdAmount(selected.progress.getNextThresholdAmount()); result.setRemainingAmount(selected.progress.getRemainingAmount()); result.setProgresses(List.of(selected.progress));
        result.setRecommendProductIds(Integer.valueOf(1).equals(activity.getShowRecommendations()) ? new ArrayList<>(scopeIds(activityId, SCOPE_RECOMMEND_PRODUCT)) : List.of());
        return result;
    }

    @Override @Transactional public void reserve(String orderNo, PromotionCheckoutResult result) {
        if (result == null || result.getActivityId() == null || result.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        if (activityMapper.reserve(result.getActivityId(), result.getDiscountAmount()) == 0) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "活动优惠名额或预算已用完，请重新结算");
        PromotionOrderReservation reservation = new PromotionOrderReservation(); reservation.setActivityId(result.getActivityId()); reservation.setOrderNo(orderNo);
        reservation.setQualifiedAmount(result.getQualifiedAmount()); reservation.setDiscountAmount(result.getDiscountAmount()); reservation.setStatus(0);
        reservation.setSnapshotJson("{\"name\":\"" + json(result.getActivityName()) + "\",\"type\":\"" + json(result.getActivityType()) + "\",\"threshold\":\"" + result.getThresholdAmount() + "\",\"qualified\":\"" + result.getQualifiedAmount() + "\",\"discount\":\"" + result.getDiscountAmount() + "\"}");
        reservationMapper.insert(reservation);
    }
    @Override @Transactional public void markPaid(String orderNo) { transition(orderNo, 1); }
    @Override @Transactional public void release(String orderNo) { transition(orderNo, 2); }
    private void transition(String orderNo, int target) {
        PromotionOrderReservation reservation = reservationMapper.selectOne(new LambdaQueryWrapper<PromotionOrderReservation>().eq(PromotionOrderReservation::getOrderNo, orderNo).last("FOR UPDATE"));
        if (reservation == null || reservation.getStatus() != 0) return;
        if (target == 1) activityMapper.markPaid(reservation.getActivityId(), reservation.getDiscountAmount()); else activityMapper.release(reservation.getActivityId(), reservation.getDiscountAmount());
        reservation.setStatus(target); reservationMapper.updateById(reservation);
    }
    private Candidate candidate(PromotionActivity activity, List<PromotionPricingItem> items) {
        Set<Long> categories = scopeIds(activity.getId(), SCOPE_CATEGORY), products = scopeIds(activity.getId(), SCOPE_PRODUCT), excluded = scopeIds(activity.getId(), SCOPE_EXCLUDED_PRODUCT);
        BigDecimal qualified = items.stream().filter(item -> item.getSubtotal() != null && !excluded.contains(item.getProductId())).filter(item -> activity.getScopeType() == 0 || (activity.getScopeType() == 1 && categories.contains(item.getCategoryId())) || (activity.getScopeType() == 2 && products.contains(item.getProductId()))).map(PromotionPricingItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PromotionThreshold> thresholds = thresholdMapper.selectList(new LambdaQueryWrapper<PromotionThreshold>().eq(PromotionThreshold::getActivityId, activity.getId()).orderByDesc(PromotionThreshold::getThresholdAmount));
        PromotionThreshold hit = thresholds.stream().filter(t -> qualified.compareTo(t.getThresholdAmount()) >= 0).findFirst().orElse(null);
        PromotionThreshold next = thresholds.stream().filter(t -> qualified.compareTo(t.getThresholdAmount()) < 0).min(Comparator.comparing(PromotionThreshold::getThresholdAmount)).orElse(null);
        BigDecimal discount = hit == null ? BigDecimal.ZERO : discount(activity, hit, qualified);
        PromotionCheckoutResult.PromotionProgress p = new PromotionCheckoutResult.PromotionProgress(); p.setActivityId(activity.getId()); p.setActivityName(activity.getName()); p.setActivityType(activity.getActivityType()); p.setQualifiedAmount(qualified); p.setDiscountAmount(discount); p.setAchieved(hit != null);
        p.setThresholdAmount(hit != null ? hit.getThresholdAmount() : next == null ? null : next.getThresholdAmount()); p.setNextThresholdAmount(next == null ? null : next.getThresholdAmount()); p.setRemainingAmount(next == null ? BigDecimal.ZERO : next.getThresholdAmount().subtract(qualified).max(BigDecimal.ZERO));
        return new Candidate(activity, qualified, hit, discount, p);
    }
    private BigDecimal discount(PromotionActivity activity, PromotionThreshold threshold, BigDecimal qualified) {
        BigDecimal value = "FULL_DISCOUNT".equals(activity.getActivityType()) ? qualified.multiply(BigDecimal.TEN.subtract(threshold.getDiscountRate())).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP) : threshold.getReductionAmount();
        if (threshold.getDiscountCap() != null) value = value.min(threshold.getDiscountCap()); return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
    private Set<Long> scopeIds(Long activityId, int type) { return scopeMapper.selectList(new LambdaQueryWrapper<PromotionScope>().eq(PromotionScope::getActivityId, activityId).eq(PromotionScope::getTargetType, type)).stream().map(PromotionScope::getTargetId).collect(Collectors.toSet()); }
    private PromotionActivity requireOwned(Long merchantId, Long id) { PromotionActivity e = activityMapper.selectById(id); if (e == null || !Objects.equals(e.getMerchantId(), merchantId)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "活动不存在或不属于当前商家"); return e; }
    private void validate(PromotionActivityRequest r) {
        if (!r.getEndAt().isAfter(r.getStartAt())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "活动结束时间必须晚于开始时间");
        for (PromotionThresholdRequest t : r.getThresholds()) { boolean reduction = "FULL_REDUCTION".equals(r.getActivityType()); if ((reduction && t.getReductionAmount() == null) || (!reduction && (t.getDiscountRate() == null || t.getDiscountRate().compareTo(BigDecimal.TEN) > 0))) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "阶梯优惠配置不完整"); }
    }
    private void copy(PromotionActivityRequest r, PromotionActivity e) { e.setName(r.getName().trim()); e.setActivityType(r.getActivityType()); e.setPriority(r.getPriority() == null ? 0 : r.getPriority()); e.setStatus(r.getStatus()); e.setStartAt(r.getStartAt()); e.setEndAt(r.getEndAt()); e.setScopeType(r.getScopeType()); e.setStackNewUserCoupon(flag(r.getStackNewUserCoupon())); e.setStackRepurchaseCoupon(flag(r.getStackRepurchaseCoupon())); e.setShowRecommendations(flag(r.getShowRecommendations())); e.setBudgetAmount(r.getBudgetAmount()); e.setMaxOrderCount(r.getMaxOrderCount()); }
    private int flag(Integer value) { return Integer.valueOf(1).equals(value) ? 1 : 0; }
    private void replaceChildren(Long activityId, PromotionActivityRequest r) { thresholdMapper.purgeByActivityId(activityId); scopeMapper.purgeByActivityId(activityId); int sort = 0; for (PromotionThresholdRequest t : r.getThresholds()) { PromotionThreshold e = new PromotionThreshold(); e.setActivityId(activityId); e.setThresholdAmount(t.getThresholdAmount()); e.setReductionAmount(t.getReductionAmount()); e.setDiscountRate(t.getDiscountRate()); e.setDiscountCap(t.getDiscountCap()); e.setSort(++sort); thresholdMapper.insert(e); } saveScopes(activityId, SCOPE_CATEGORY, r.getCategoryIds()); saveScopes(activityId, SCOPE_PRODUCT, r.getProductIds()); saveScopes(activityId, SCOPE_EXCLUDED_PRODUCT, r.getExcludedProductIds()); saveScopes(activityId, SCOPE_RECOMMEND_PRODUCT, r.getRecommendProductIds()); }
    private void saveScopes(Long id, int type, List<Long> ids) { if (ids == null) return; ids.stream().filter(Objects::nonNull).distinct().forEach(target -> { PromotionScope s = new PromotionScope(); s.setActivityId(id); s.setTargetType(type); s.setTargetId(target); scopeMapper.insert(s); }); }
    private PromotionActivityVO toVO(PromotionActivity e) { PromotionActivityVO v = new PromotionActivityVO(); v.setId(e.getId()); v.setName(e.getName()); v.setActivityType(e.getActivityType()); v.setPriority(e.getPriority()); v.setStatus(e.getStatus()); v.setStartAt(e.getStartAt()); v.setEndAt(e.getEndAt()); v.setScopeType(e.getScopeType()); v.setStackNewUserCoupon(e.getStackNewUserCoupon()); v.setStackRepurchaseCoupon(e.getStackRepurchaseCoupon()); v.setShowRecommendations(e.getShowRecommendations()); v.setBudgetAmount(e.getBudgetAmount()); v.setMaxOrderCount(e.getMaxOrderCount()); v.setReservedBudget(e.getReservedBudget()); v.setReservedOrderCount(e.getReservedOrderCount()); v.setPaidBudget(e.getPaidBudget()); v.setPaidOrderCount(e.getPaidOrderCount()); v.setCategoryIds(new ArrayList<>(scopeIds(e.getId(), SCOPE_CATEGORY))); v.setProductIds(new ArrayList<>(scopeIds(e.getId(), SCOPE_PRODUCT))); v.setExcludedProductIds(new ArrayList<>(scopeIds(e.getId(), SCOPE_EXCLUDED_PRODUCT))); v.setRecommendProductIds(new ArrayList<>(scopeIds(e.getId(), SCOPE_RECOMMEND_PRODUCT))); v.setThresholds(thresholdMapper.selectList(new LambdaQueryWrapper<PromotionThreshold>().eq(PromotionThreshold::getActivityId, e.getId()).orderByAsc(PromotionThreshold::getSort)).stream().map(t -> { PromotionThresholdRequest d = new PromotionThresholdRequest(); d.setThresholdAmount(t.getThresholdAmount()); d.setReductionAmount(t.getReductionAmount()); d.setDiscountRate(t.getDiscountRate()); d.setDiscountCap(t.getDiscountCap()); return d; }).toList()); return v; }
    private String json(String source) { return source == null ? "" : source.replace("\\", "\\\\").replace("\"", "\\\""); }
    private record Candidate(PromotionActivity activity, BigDecimal qualified, PromotionThreshold threshold, BigDecimal discount, PromotionCheckoutResult.PromotionProgress progress) {}
}
