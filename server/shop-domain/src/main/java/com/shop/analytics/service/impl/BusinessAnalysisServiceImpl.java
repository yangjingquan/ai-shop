package com.shop.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.analytics.dto.BusinessAnalysisVO;
import com.shop.analytics.entity.AnalyticsEvent;
import com.shop.analytics.entity.OrderAttribution;
import com.shop.analytics.mapper.AnalyticsEventMapper;
import com.shop.analytics.mapper.OrderAttributionMapper;
import com.shop.analytics.service.BusinessAnalysisService;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.journey.entity.MarketingJourney;
import com.shop.journey.entity.MarketingJourneyExecution;
import com.shop.journey.mapper.MarketingJourneyExecutionMapper;
import com.shop.journey.mapper.MarketingJourneyMapper;
import com.shop.marketing.entity.PromotionActivity;
import com.shop.marketing.mapper.PromotionActivityMapper;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.referral.entity.ReferralRelation;
import com.shop.referral.mapper.ReferralRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class BusinessAnalysisServiceImpl implements BusinessAnalysisService {
    private final AnalyticsEventMapper eventMapper; private final OrderAttributionMapper attributionMapper;
    private final OrderMapper orderMapper; private final RefundApplicationMapper refundMapper; private final CartItemMapper cartMapper; private final PaymentLogMapper paymentLogMapper;
    private final ReferralRelationMapper referralRelationMapper; private final MarketingJourneyExecutionMapper journeyExecutionMapper; private final MarketingJourneyMapper journeyMapper;
    private final PromotionActivityMapper promotionMapper; private final CouponTemplateMapper couponTemplateMapper; private final MerchantMapper merchantMapper;

    @Override @Transactional
    public void recordProductView(Long merchantId, Long userId, String eventId, Long productId, String channelCode) {
        if (merchantId == null || userId == null || productId == null || eventId == null || !eventId.matches("[A-Za-z0-9_-]{8,64}")) return;
        AnalyticsEvent value = new AnalyticsEvent(); value.setMerchantId(merchantId); value.setUserId(userId); value.setEventId(eventId); value.setEventType("PRODUCT_VIEW"); value.setProductId(productId); value.setChannelCode(normalizeChannelCode(channelCode)); value.setOccurredAt(LocalDateTime.now());
        try { eventMapper.insert(value); } catch (DuplicateKeyException ignored) { }
    }

    @Override @Transactional
    public void snapshotOrder(Long merchantId, String orderNo) {
        if (merchantId == null || orderNo == null) return;
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId).eq(Order::getOrderNo, orderNo));
        if (order == null) return;
        Attribution resolved = resolve(order);
        OrderAttribution current = attributionMapper.selectOne(new LambdaQueryWrapper<OrderAttribution>().eq(OrderAttribution::getOrderNo, orderNo));
        if (current == null) { current = new OrderAttribution(); current.setMerchantId(merchantId); current.setOrderNo(orderNo); current.setAttributedAt(LocalDateTime.now()); attributionMapper.insert(apply(current, resolved)); }
    }

    @Override
    public BusinessAnalysisVO report(Long merchantId, LocalDateTime from, LocalDateTime to) {
        LocalDateTime safeTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime safeFrom = from == null ? safeTo.minusDays(30) : from;
        if (!safeFrom.isBefore(safeTo) || safeFrom.isBefore(safeTo.minusDays(366))) throw new IllegalArgumentException("统计区间需在 1 至 366 天内");
        List<Order> paid = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(merchantId != null, Order::getMerchantId, merchantId).isNotNull(Order::getPayTime).ge(Order::getPayTime, safeFrom).lt(Order::getPayTime, safeTo));
        List<RefundApplication> refunds = refundMapper.selectList(new LambdaQueryWrapper<RefundApplication>().eq(merchantId != null, RefundApplication::getMerchantId, merchantId).eq(RefundApplication::getStatus, RefundStatus.SUCCESS.getCode()).ge(RefundApplication::getRefundTime, safeFrom).lt(RefundApplication::getRefundTime, safeTo));
        Map<String, BigDecimal> refundByOrder = refunds.stream().collect(Collectors.groupingBy(RefundApplication::getOrderNo, Collectors.reducing(BigDecimal.ZERO, value -> money(value.getRefundAmount()), BigDecimal::add)));
        Map<String, OrderAttribution> snapshots = attributionMapper.selectList(new LambdaQueryWrapper<OrderAttribution>().eq(merchantId != null, OrderAttribution::getMerchantId, merchantId).in(!paid.isEmpty(), OrderAttribution::getOrderNo, paid.stream().map(Order::getOrderNo).toList())).stream().collect(Collectors.toMap(OrderAttribution::getOrderNo, Function.identity(), (a,b)->a));
        Map<String, Aggregate> sources = new LinkedHashMap<>();
        for (Order order : paid) {
            OrderAttribution source = snapshots.get(order.getOrderNo());
            if (source == null) { snapshotOrder(order.getMerchantId(), order.getOrderNo()); source = attributionMapper.selectOne(new LambdaQueryWrapper<OrderAttribution>().eq(OrderAttribution::getOrderNo, order.getOrderNo())); if (source != null) snapshots.put(order.getOrderNo(), source); }
            String key = source == null ? "NATURAL" : source.getPrimarySource(); String name = source == null ? "自然成交" : source.getSourceName();
            Aggregate aggregate = sources.computeIfAbsent(key + ":" + name, ignored -> new Aggregate(key, name));
            aggregate.orders++; aggregate.paid = aggregate.paid.add(money(order.getPayAmount())); aggregate.refund = aggregate.refund.add(refundByOrder.getOrDefault(order.getOrderNo(), BigDecimal.ZERO)); aggregate.discount = aggregate.discount.add(money(order.getCouponDiscountAmount()).add(money(order.getPromotionDiscountAmount())).add(money(order.getBundleDiscountAmount())));
        }
        BigDecimal paidAmount = paid.stream().map(Order::getPayAmount).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = refunds.stream().map(RefundApplication::getRefundAmount).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidUsers = paid.stream().map(Order::getUserId).filter(Objects::nonNull).distinct().count();
        long viewUsers = eventMapper.selectList(new LambdaQueryWrapper<AnalyticsEvent>().eq(merchantId != null, AnalyticsEvent::getMerchantId, merchantId).eq(AnalyticsEvent::getEventType, "PRODUCT_VIEW").ge(AnalyticsEvent::getOccurredAt, safeFrom).lt(AnalyticsEvent::getOccurredAt, safeTo)).stream().map(AnalyticsEvent::getUserId).distinct().count();
        long cartUsers = cartMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(merchantId != null, CartItem::getMerchantId, merchantId).ge(CartItem::getCreatedAt, safeFrom).lt(CartItem::getCreatedAt, safeTo)).stream().map(CartItem::getUserId).distinct().count();
        long submitUsers = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(merchantId != null, Order::getMerchantId, merchantId).ge(Order::getCreatedAt, safeFrom).lt(Order::getCreatedAt, safeTo)).stream().map(Order::getUserId).distinct().count();
        BusinessAnalysisVO result = new BusinessAnalysisVO(); result.setFrom(safeFrom); result.setTo(safeTo); result.setDataAsOf(LocalDateTime.now()); result.setMetricDefinition("支付按订单支付成功时间；退款按退款成功时间；净成交额=支付金额-退款成功金额；主归因仅使用直接证据，支付前 7 天内最近一次自动营销触达仅记助攻、不分摊金额；时区 Asia/Shanghai。");
        BusinessAnalysisVO.Overview overview = new BusinessAnalysisVO.Overview(); overview.setPaidOrderCount(paid.size()); overview.setPaidUserCount(paidUsers); overview.setPaidAmount(paidAmount); overview.setRefundAmount(refundAmount); overview.setNetAmount(paidAmount.subtract(refundAmount)); overview.setActualDiscountAmount(paid.stream().map(order -> money(order.getCouponDiscountAmount()).add(money(order.getPromotionDiscountAmount())).add(money(order.getBundleDiscountAmount()))).reduce(BigDecimal.ZERO, BigDecimal::add)); overview.setAverageOrderValue(divide(paidAmount, paid.size())); overview.setRefundRate(divide(refundAmount, paidAmount)); overview.setAttributionCoverage(divide(BigDecimal.valueOf(paid.stream().filter(order -> snapshots.containsKey(order.getOrderNo())).count()), paid.size())); overview.setUnmatchedPaymentCount(merchantId == null ? Optional.ofNullable(paymentLogMapper.selectUnmatchedPaymentCount(safeFrom, safeTo)).orElse(0L) : 0L); result.setOverview(overview);
        BusinessAnalysisVO.Funnel funnel = new BusinessAnalysisVO.Funnel(); funnel.setProductViewUsers(viewUsers); funnel.setAddCartUsers(cartUsers); funnel.setSubmitOrderUsers(submitUsers); funnel.setPaidUsers(paidUsers); funnel.setAddCartRate(divide(BigDecimal.valueOf(cartUsers), viewUsers)); funnel.setSubmitRate(divide(BigDecimal.valueOf(submitUsers), cartUsers)); funnel.setPayRate(divide(BigDecimal.valueOf(paidUsers), submitUsers)); funnel.setCoverageNote(viewUsers == 0 ? "商品浏览事件从本版本发布后开始统计；历史浏览不补造。" : "商品浏览为事件口径；加购、提交订单、支付为业务流水口径。"); result.setFunnel(funnel);
        result.setSources(sources.values().stream().map(value -> { BusinessAnalysisVO.SourceRow row = new BusinessAnalysisVO.SourceRow(); row.setSource(value.source); row.setSourceName(value.name); row.setPaidOrderCount(value.orders); row.setPaidAmount(value.paid); row.setRefundAmount(value.refund); row.setNetAmount(value.paid.subtract(value.refund)); row.setActualDiscountAmount(value.discount); row.setCoverage(divide(BigDecimal.valueOf(value.orders), paid.size())); return row; }).sorted(Comparator.comparing(BusinessAnalysisVO.SourceRow::getPaidAmount).reversed()).toList());
        result.setMerchantPerformance(merchantId == null ? merchantRows(paid, refundByOrder) : List.of());
        return result;
    }

    private List<BusinessAnalysisVO.MerchantRow> merchantRows(List<Order> paid, Map<String, BigDecimal> refundByOrder) { Set<Long> ids = paid.stream().map(Order::getMerchantId).filter(Objects::nonNull).collect(Collectors.toSet()); Map<Long, String> names = ids.isEmpty() ? Map.of() : merchantMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Merchant::getId, Merchant::getName)); Map<Long, Aggregate> aggregates = new HashMap<>(); for (Order order : paid) { Aggregate value = aggregates.computeIfAbsent(order.getMerchantId(), ignored -> new Aggregate("", "")); value.orders++; value.paid = value.paid.add(money(order.getPayAmount())); value.refund = value.refund.add(refundByOrder.getOrDefault(order.getOrderNo(), BigDecimal.ZERO)); } return aggregates.entrySet().stream().map(entry -> { BusinessAnalysisVO.MerchantRow row = new BusinessAnalysisVO.MerchantRow(); row.setMerchantId(entry.getKey()); row.setMerchantName(names.getOrDefault(entry.getKey(), "商户 #" + entry.getKey())); row.setPaidOrderCount(entry.getValue().orders); row.setPaidAmount(entry.getValue().paid); row.setRefundAmount(entry.getValue().refund); row.setNetAmount(entry.getValue().paid.subtract(entry.getValue().refund)); return row; }).sorted(Comparator.comparing(BusinessAnalysisVO.MerchantRow::getNetAmount).reversed()).toList(); }

    private Attribution resolve(Order order) {
        ReferralRelation referral = referralRelationMapper.selectOne(new LambdaQueryWrapper<ReferralRelation>().eq(ReferralRelation::getMerchantId, order.getMerchantId()).eq(ReferralRelation::getFirstOrderNo, order.getOrderNo()));
        if (referral != null) return withRecentJourneyAssist(order, new Attribution("REFERRAL", referral.getCampaignId(), "邀请有礼", "REFERRAL_FIRST_ORDER", ""));
        if (order.getOrderType() != null && order.getOrderType() != 0) return withRecentJourneyAssist(order, new Attribution("ACTIVITY", activityId(order), activityName(order), "ORDER_ACTIVITY", ""));
        if (order.getCouponId() != null) { MarketingJourneyExecution execution = journeyExecutionMapper.selectOne(new LambdaQueryWrapper<MarketingJourneyExecution>().eq(MarketingJourneyExecution::getCouponId, order.getCouponId())); if (execution != null) { MarketingJourney journey = journeyMapper.selectById(execution.getJourneyId()); return new Attribution("JOURNEY", execution.getJourneyId(), journey == null ? "自动营销" : journey.getName(), "JOURNEY_COUPON_USED", ""); } }
        if (order.getPromotionActivityId() != null) { PromotionActivity activity = promotionMapper.selectById(order.getPromotionActivityId()); return withRecentJourneyAssist(order, new Attribution("ACTIVITY", order.getPromotionActivityId(), activity == null ? "营销活动" : activity.getName(), "PROMOTION_ORDER", "")); }
        if (order.getCouponTemplateId() != null) { CouponTemplate coupon = couponTemplateMapper.selectById(order.getCouponTemplateId()); return withRecentJourneyAssist(order, new Attribution("COUPON", order.getCouponTemplateId(), coupon == null ? "优惠券" : coupon.getName(), "COUPON_USED", "")); }
        return withRecentJourneyAssist(order, new Attribution("NATURAL", null, "自然成交", "NO_DIRECT_EVIDENCE", ""));
    }
    private Attribution withRecentJourneyAssist(Order order, Attribution primary) { LocalDateTime convertedAt = order.getPayTime() == null ? order.getCreatedAt() : order.getPayTime(); if (convertedAt == null || order.getUserId() == null || "JOURNEY".equals(primary.source)) return primary; List<MarketingJourneyExecution> executions = journeyExecutionMapper.selectList(new LambdaQueryWrapper<MarketingJourneyExecution>().eq(MarketingJourneyExecution::getMerchantId, order.getMerchantId()).eq(MarketingJourneyExecution::getUserId, order.getUserId()).isNotNull(MarketingJourneyExecution::getExecutedAt).ge(MarketingJourneyExecution::getExecutedAt, convertedAt.minusDays(7)).le(MarketingJourneyExecution::getExecutedAt, convertedAt).orderByDesc(MarketingJourneyExecution::getExecutedAt).last("LIMIT 1")); if (executions == null || executions.isEmpty()) return primary; MarketingJourneyExecution execution = executions.get(0); MarketingJourney journey = journeyMapper.selectById(execution.getJourneyId()); String name = journey == null ? "自动营销" : journey.getName(); return new Attribution(primary.source, primary.id, primary.name, primary.evidence, "JOURNEY:" + execution.getJourneyId() + ":" + name); }
    private OrderAttribution apply(OrderAttribution target, Attribution source) { target.setPrimarySource(source.source); target.setSourceId(source.id); target.setSourceName(source.name); target.setEvidenceType(source.evidence); target.setAssistSource(source.assist); return target; }
    private Long activityId(Order order) { if (order.getGroupBuyGroupId() != null) return order.getGroupBuyGroupId(); if (order.getSeckillSessionId() != null) return order.getSeckillSessionId(); if (order.getBundleActivityId() != null) return order.getBundleActivityId(); if (order.getPresaleOrderId() != null) return order.getPresaleOrderId(); return order.getPromotionActivityId(); }
    private String activityName(Order order) { return switch (order.getOrderType()) { case 1 -> "拼团活动"; case 2 -> "秒杀活动"; case 4 -> "搭配购活动"; case 6 -> "预售活动"; default -> "营销活动"; }; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String normalizeChannelCode(String value) { return value != null && value.matches("[A-Za-z0-9_-]{1,32}") ? value : null; }
    private BigDecimal divide(BigDecimal numerator, long denominator) { return denominator <= 0 ? BigDecimal.ZERO : numerator.divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP); }
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) { return denominator == null || denominator.signum() <= 0 ? BigDecimal.ZERO : numerator.divide(denominator, 4, RoundingMode.HALF_UP); }
    private record Attribution(String source, Long id, String name, String evidence, String assist) { } private static class Aggregate { final String source, name; long orders; BigDecimal paid=BigDecimal.ZERO, refund=BigDecimal.ZERO, discount=BigDecimal.ZERO; Aggregate(String source,String name){this.source=source;this.name=name;} }
}
