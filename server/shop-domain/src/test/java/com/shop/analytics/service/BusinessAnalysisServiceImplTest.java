package com.shop.analytics.service;

import com.shop.analytics.dto.BusinessAnalysisVO;
import com.shop.analytics.entity.AnalyticsEvent;
import com.shop.analytics.entity.OrderAttribution;
import com.shop.analytics.mapper.AnalyticsEventMapper;
import com.shop.analytics.mapper.OrderAttributionMapper;
import com.shop.analytics.service.impl.BusinessAnalysisServiceImpl;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.journey.entity.MarketingJourney;
import com.shop.journey.entity.MarketingJourneyExecution;
import com.shop.journey.mapper.MarketingJourneyExecutionMapper;
import com.shop.journey.mapper.MarketingJourneyMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessAnalysisServiceImplTest {
    @Mock private AnalyticsEventMapper eventMapper;
    @Mock private OrderAttributionMapper attributionMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private RefundApplicationMapper refundMapper;
    @Mock private CartItemMapper cartMapper;
    @Mock private PaymentLogMapper paymentLogMapper;
    @Mock private ReferralRelationMapper referralMapper;
    @Mock private MarketingJourneyExecutionMapper journeyExecutionMapper;
    @Mock private MarketingJourneyMapper journeyMapper;
    @Mock private PromotionActivityMapper promotionMapper;
    @Mock private CouponTemplateMapper couponTemplateMapper;
    @Mock private MerchantMapper merchantMapper;
    private BusinessAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BusinessAnalysisServiceImpl(eventMapper, attributionMapper, orderMapper, refundMapper, cartMapper,
                paymentLogMapper, referralMapper, journeyExecutionMapper, journeyMapper, promotionMapper, couponTemplateMapper, merchantMapper);
    }

    @Test
    void reportUsesPaidAndSuccessfulRefundLedgersAndKeepsOnePrimarySourcePerOrder() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = from.plusDays(30);
        Order order = order("O-1", 7L, from.plusDays(2), new BigDecimal("100.00"));
        order.setCouponDiscountAmount(new BigDecimal("10.00"));
        order.setPromotionDiscountAmount(new BigDecimal("5.00"));
        RefundApplication refund = new RefundApplication();
        refund.setOrderNo("O-1"); refund.setRefundAmount(new BigDecimal("20.00")); refund.setStatus(RefundStatus.SUCCESS.getCode());
        OrderAttribution attribution = new OrderAttribution();
        attribution.setOrderNo("O-1"); attribution.setPrimarySource("COUPON"); attribution.setSourceName("新客券");
        AnalyticsEvent event = new AnalyticsEvent(); event.setUserId(7L);
        CartItem cart = new CartItem(); cart.setUserId(7L);

        when(orderMapper.selectList(any())).thenReturn(List.of(order), List.of(order));
        when(refundMapper.selectList(any())).thenReturn(List.of(refund));
        when(attributionMapper.selectList(any())).thenReturn(List.of(attribution));
        when(eventMapper.selectList(any())).thenReturn(List.of(event));
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
        when(paymentLogMapper.selectUnmatchedPaymentCount(from, to)).thenReturn(2L);
        Merchant merchant = new Merchant(); merchant.setId(1L); merchant.setName("演示商户");
        when(merchantMapper.selectBatchIds(any())).thenReturn(List.of(merchant));

        BusinessAnalysisVO result = service.report(null, from, to);

        assertMoney("100.00", result.getOverview().getPaidAmount());
        assertMoney("20.00", result.getOverview().getRefundAmount());
        assertMoney("80.00", result.getOverview().getNetAmount());
        assertMoney("15.00", result.getOverview().getActualDiscountAmount());
        assertMoney("0.2000", result.getOverview().getRefundRate());
        assertMoney("1.0000", result.getOverview().getAttributionCoverage());
        assertEquals(2L, result.getOverview().getUnmatchedPaymentCount());
        assertEquals(1, result.getSources().size());
        assertEquals("COUPON", result.getSources().get(0).getSource());
        assertMoney("80.00", result.getSources().get(0).getNetAmount());
        assertEquals(1L, result.getFunnel().getProductViewUsers());
        assertEquals(1L, result.getFunnel().getAddCartUsers());
        assertEquals(1L, result.getFunnel().getSubmitOrderUsers());
        assertEquals(1L, result.getFunnel().getPaidUsers());
        assertEquals("演示商户", result.getMerchantPerformance().get(0).getMerchantName());
    }

    @Test
    void snapshotUsesReferralAsPrimaryAndStoresRecentJourneyAsAssistOnly() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 22, 12, 0);
        Order order = order("O-2", 8L, now, new BigDecimal("50.00"));
        ReferralRelation referral = new ReferralRelation(); referral.setCampaignId(9L);
        MarketingJourneyExecution execution = new MarketingJourneyExecution(); execution.setJourneyId(33L); execution.setExecutedAt(now.minusDays(2));
        MarketingJourney journey = new MarketingJourney(); journey.setName("召回营销");
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(attributionMapper.selectOne(any())).thenReturn(null);
        when(referralMapper.selectOne(any())).thenReturn(referral);
        when(journeyExecutionMapper.selectList(any())).thenReturn(List.of(execution));
        when(journeyMapper.selectById(33L)).thenReturn(journey);

        service.snapshotOrder(1L, "O-2");

        ArgumentCaptor<OrderAttribution> saved = ArgumentCaptor.forClass(OrderAttribution.class);
        verify(attributionMapper).insert(saved.capture());
        assertEquals("REFERRAL", saved.getValue().getPrimarySource());
        assertEquals("REFERRAL_FIRST_ORDER", saved.getValue().getEvidenceType());
        assertEquals("JOURNEY:33:召回营销", saved.getValue().getAssistSource());
    }

    @Test
    void directJourneyCouponWinsPrimaryAttributionAndDoesNotDuplicateItAsAssist() {
        Order order = order("O-3", 9L, LocalDateTime.of(2026, 9, 22, 12, 0), new BigDecimal("30.00"));
        order.setCouponId(500L);
        MarketingJourneyExecution execution = new MarketingJourneyExecution(); execution.setJourneyId(34L);
        MarketingJourney journey = new MarketingJourney(); journey.setName("支付召回");
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(attributionMapper.selectOne(any())).thenReturn(null);
        when(referralMapper.selectOne(any())).thenReturn(null);
        when(journeyExecutionMapper.selectOne(any())).thenReturn(execution);
        when(journeyMapper.selectById(34L)).thenReturn(journey);

        service.snapshotOrder(1L, "O-3");

        ArgumentCaptor<OrderAttribution> saved = ArgumentCaptor.forClass(OrderAttribution.class);
        verify(attributionMapper).insert(saved.capture());
        assertEquals("JOURNEY", saved.getValue().getPrimarySource());
        assertEquals("JOURNEY_COUPON_USED", saved.getValue().getEvidenceType());
        assertEquals("", saved.getValue().getAssistSource());
        verify(journeyExecutionMapper, never()).selectList(any());
    }

    @Test
    void productViewIsIdempotentAndOnlyPersistsSafeReservedChannelCode() {
        doThrow(new org.springframework.dao.DuplicateKeyException("duplicate")).when(eventMapper).insert(any());

        service.recordProductView(1L, 2L, "pv_abcde123", 3L, "share_qr");
        service.recordProductView(1L, 2L, "pv_abcde123", 3L, "invalid channel code");

        ArgumentCaptor<AnalyticsEvent> saved = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(eventMapper, org.mockito.Mockito.times(2)).insert(saved.capture());
        assertEquals("share_qr", saved.getAllValues().get(0).getChannelCode());
        assertNull(saved.getAllValues().get(1).getChannelCode());
    }

    @Test
    void reportCountsNewlyBackfilledAttributionInCoverageOnTheFirstRead() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = from.plusDays(7);
        Order order = order("O-4", 10L, from.plusDays(1), new BigDecimal("12.00"));
        OrderAttribution backfilled = new OrderAttribution();
        backfilled.setOrderNo("O-4"); backfilled.setPrimarySource("NATURAL"); backfilled.setSourceName("自然成交");
        when(orderMapper.selectList(any())).thenReturn(List.of(order), List.of(order));
        when(refundMapper.selectList(any())).thenReturn(List.of());
        when(attributionMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(attributionMapper.selectOne(any())).thenReturn(null, backfilled);
        when(referralMapper.selectOne(any())).thenReturn(null);
        when(journeyExecutionMapper.selectList(any())).thenReturn(List.of());
        when(eventMapper.selectList(any())).thenReturn(List.of());
        when(cartMapper.selectList(any())).thenReturn(List.of());

        BusinessAnalysisVO result = service.report(1L, from, to);

        assertMoney("1.0000", result.getOverview().getAttributionCoverage());
        assertEquals("NATURAL", result.getSources().get(0).getSource());
        assertEquals(0L, result.getOverview().getUnmatchedPaymentCount());
    }

    private Order order(String orderNo, Long userId, LocalDateTime payTime, BigDecimal payAmount) {
        Order value = new Order(); value.setMerchantId(1L); value.setOrderNo(orderNo); value.setUserId(userId); value.setPayTime(payTime); value.setCreatedAt(payTime.minusHours(1)); value.setPayAmount(payAmount); return value;
    }
    private void assertMoney(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
}
