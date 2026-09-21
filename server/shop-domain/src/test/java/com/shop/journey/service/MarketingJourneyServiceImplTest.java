package com.shop.journey.service;

import com.shop.coupon.service.CouponService;
import com.shop.customer.entity.CustomerMetricSnapshot;
import com.shop.customer.mapper.CustomerMetricSnapshotMapper;
import com.shop.customer.service.CustomerOperationsService;
import com.shop.journey.dto.MarketingJourneyRequest;
import com.shop.journey.entity.MarketingJourney;
import com.shop.journey.entity.MarketingJourneyEnrollment;
import com.shop.journey.entity.MarketingJourneyExecution;
import com.shop.journey.mapper.MarketingJourneyEnrollmentMapper;
import com.shop.journey.mapper.MarketingJourneyExecutionMapper;
import com.shop.journey.mapper.MarketingJourneyMapper;
import com.shop.journey.service.impl.MarketingJourneyServiceImpl;
import com.shop.notification.service.UserNotificationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingJourneyServiceImplTest {
    @Mock private MarketingJourneyMapper journeyMapper;
    @Mock private MarketingJourneyEnrollmentMapper enrollmentMapper;
    @Mock private MarketingJourneyExecutionMapper executionMapper;
    @Mock private CustomerMetricSnapshotMapper metricMapper;
    @Mock private CustomerOperationsService customerOperationsService;
    @Mock private CouponService couponService;
    @Mock private UserNotificationService notificationService;
    private MarketingJourneyService service;

    @BeforeEach
    void setUp() {
        service = new MarketingJourneyServiceImpl(journeyMapper, enrollmentMapper, executionMapper, metricMapper,
                customerOperationsService, couponService, notificationService);
    }

    @Test
    void scanEnqueuesOnlyCurrentAudienceAndUsesPaymentTimeAsIdempotencyKey() {
        MarketingJourney journey = journey("ORDER_PAID");
        CustomerMetricSnapshot paid = metric(101L);
        CustomerMetricSnapshot notInAudience = metric(102L);
        when(journeyMapper.selectById(1L)).thenReturn(journey);
        when(customerOperationsService.segmentUserIds(10L, null)).thenReturn(List.of(101L));
        when(metricMapper.selectList(any())).thenReturn(List.of(paid, notInAudience));
        when(enrollmentMapper.selectCount(any())).thenReturn(0L);
        when(executionMapper.selectCount(any())).thenReturn(0L);

        service.scan(10L, 1L);

        ArgumentCaptor<MarketingJourneyEnrollment> captor = ArgumentCaptor.forClass(MarketingJourneyEnrollment.class);
        verify(enrollmentMapper).insert(captor.capture());
        MarketingJourneyEnrollment enrollment = captor.getValue();
        assertEquals(101L, enrollment.getUserId());
        assertEquals("ORDER_PAID:20260921090000", enrollment.getTriggerKey());
        assertEquals(paid.getLastPaidAt(), enrollment.getTriggerAt());
        verify(customerOperationsService).refreshMerchantMetrics(10L);
    }

    @Test
    void dueEnrollmentIssuesCouponAndMessageThenRecordsSuccess() {
        MarketingJourney journey = journey("BROWSE_NO_PURCHASE");
        journey.setCouponTemplateId(88L);
        journey.setNotificationTitle("专属优惠");
        journey.setNotificationContent("现在下单可享优惠");
        MarketingJourneyEnrollment enrollment = new MarketingJourneyEnrollment();
        enrollment.setId(9L); enrollment.setMerchantId(10L); enrollment.setJourneyId(1L); enrollment.setUserId(101L);
        enrollment.setTriggerAt(LocalDateTime.of(2026, 9, 21, 9, 0)); enrollment.setExecuteAt(LocalDateTime.now().minusMinutes(1)); enrollment.setStatus("PENDING");
        when(enrollmentMapper.selectList(any())).thenReturn(List.of(enrollment));
        when(journeyMapper.selectById(1L)).thenReturn(journey);
        when(metricMapper.selectOne(any())).thenReturn(metric(101L));
        when(customerOperationsService.matchesSegment(10L, null, 101L)).thenReturn(true);
        when(couponService.issueTemplate(101L, 10L, 88L)).thenReturn(201L);
        when(notificationService.sendMarketing(eq(101L), eq(10L), eq("专属优惠"), eq("现在下单可享优惠"), any())).thenReturn(301L);

        service.processDue();

        ArgumentCaptor<MarketingJourneyExecution> executionCaptor = ArgumentCaptor.forClass(MarketingJourneyExecution.class);
        verify(executionMapper).insert(executionCaptor.capture());
        assertEquals(201L, executionCaptor.getValue().getCouponId());
        assertEquals(301L, executionCaptor.getValue().getNotificationId());
        assertEquals("SUCCESS", executionCaptor.getValue().getStatus());
        assertEquals("SUCCESS", enrollment.getStatus());
        verify(enrollmentMapper).updateById(enrollment);
    }

    @Test
    void retryWithExistingExecutionDoesNotIssueDuplicateReward() {
        MarketingJourneyEnrollment enrollment = new MarketingJourneyEnrollment();
        enrollment.setId(9L); enrollment.setMerchantId(10L); enrollment.setJourneyId(1L); enrollment.setUserId(101L);
        enrollment.setExecuteAt(LocalDateTime.now().minusMinutes(1)); enrollment.setStatus("PENDING");
        MarketingJourneyExecution existing = new MarketingJourneyExecution(); existing.setEnrollmentId(9L); existing.setStatus("SUCCESS");
        when(enrollmentMapper.selectList(any())).thenReturn(List.of(enrollment));
        when(executionMapper.selectOne(any())).thenReturn(existing);

        service.processDue();

        assertEquals("SUCCESS", enrollment.getStatus());
        verify(enrollmentMapper).updateById(enrollment);
        verifyNoInteractions(couponService, notificationService);
        verify(executionMapper, never()).insert(any());
    }

    @Test
    void validationRejectsJourneyWithoutReachableAction() {
        MarketingJourneyRequest request = new MarketingJourneyRequest();
        request.setName("无动作旅程"); request.setTriggerType("REGISTER"); request.setDelayMinutes(0); request.setFrequencyDays(7); request.setStopOnPaid(1); request.setStatus(1);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.save(10L, null, request));
        verifyNoInteractions(journeyMapper);
    }

    private MarketingJourney journey(String trigger) {
        MarketingJourney value = new MarketingJourney();
        value.setId(1L); value.setMerchantId(10L); value.setName("测试旅程"); value.setTriggerType(trigger); value.setDelayMinutes(0); value.setFrequencyDays(7); value.setStopOnPaid(0); value.setStatus(1); value.setCreatedAt(LocalDateTime.of(2026, 9, 21, 8, 0)); return value;
    }

    private CustomerMetricSnapshot metric(Long userId) {
        CustomerMetricSnapshot value = new CustomerMetricSnapshot();
        value.setMerchantId(10L); value.setUserId(userId); value.setPaidOrderCount(1); value.setTotalPaidAmount(BigDecimal.TEN); value.setMemberLevel(1); value.setPointsBalance(0); value.setUnusedCouponCount(0); value.setExpiringCouponCount(0); value.setFavoriteCount(0); value.setHistoryCount(0); value.setLastPaidAt(LocalDateTime.of(2026, 9, 21, 9, 0)); value.setLastViewedAt(LocalDateTime.of(2026, 9, 21, 9, 0)); value.setCalculatedAt(LocalDateTime.now()); return value;
    }
}
