package com.shop.marketing.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shop.marketing.dto.PromotionActivityRequest;
import com.shop.marketing.dto.PromotionCheckoutResult;
import com.shop.marketing.dto.PromotionPricingItem;
import com.shop.marketing.dto.PromotionThresholdRequest;
import com.shop.marketing.entity.PromotionActivity;
import com.shop.marketing.entity.PromotionScope;
import com.shop.marketing.entity.PromotionThreshold;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.mapper.PromotionActivityMapper;
import com.shop.marketing.mapper.PromotionOrderReservationMapper;
import com.shop.marketing.mapper.PromotionScopeMapper;
import com.shop.marketing.mapper.PromotionThresholdMapper;
import com.shop.marketing.service.impl.PromotionServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {
    @Mock private PromotionActivityMapper activityMapper;
    @Mock private PromotionThresholdMapper thresholdMapper;
    @Mock private PromotionScopeMapper scopeMapper;
    @Mock private PromotionOrderReservationMapper reservationMapper;
    @Mock private MarketingFeatureService marketingFeatureService;

    @BeforeAll
    static void initializeQueryMetadata() {
        for (Class<?> entity : List.of(PromotionThreshold.class, PromotionScope.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "promotion-test"), entity);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "110.88, 2, 120.00, 0.00, 9.12, false",
            "118.99, 2, 120.00, 0.00, 1.01, false",
            "119.00, 1, 119.00, 2.00, 20.00, true",
            "119.99, 1, 119.00, 2.00, 19.01, true",
            "120.00, 2, 120.00, 8.00, 80.00, true",
            "139.00, 2, 120.00, 8.00, 61.00, true",
            "200.00, 2, 200.00, 20.00, 300.00, true",
            "500.00, 2, 500.00, 120.00, 0.00, true"
    })
    void selectsPromotionAndPreservesDiscountsAcrossThresholds(String amount, long activityId,
            String threshold, String discount, String remaining, boolean achieved) {
        PromotionActivity fullDiscount = activity(2L, "FULL_DISCOUNT", 10);
        PromotionActivity fullReduction = activity(1L, "FULL_REDUCTION", 0);
        when(marketingFeatureService.isEnabled(2L, MarketingActivityCode.FULL_REDUCTION)).thenReturn(true);
        when(activityMapper.selectList(any())).thenReturn(List.of(fullDiscount, fullReduction));
        stubPromotionDetails();

        PromotionCheckoutResult result = service().calculate(2L, items(amount));

        assertThat(result.getActivityId()).isEqualTo(activityId);
        assertThat(result.getActivityType()).isEqualTo(activityId == 2L ? "FULL_DISCOUNT" : "FULL_REDUCTION");
        assertThat(result.getQualifiedAmount()).isEqualByComparingTo(amount);
        assertThat(result.getThresholdAmount()).isEqualByComparingTo(threshold);
        assertThat(result.getDiscountAmount()).isEqualByComparingTo(discount);
        assertThat(result.getRemainingAmount()).isEqualByComparingTo(remaining);
        assertThat(result.getRecommendProductIds()).containsExactlyInAnyOrderElementsOf(
                activityId == 2L ? List.of(478L, 480L, 481L, 482L) : List.of(479L, 480L, 481L, 482L, 483L, 484L));
        assertThat(result.getProgresses()).hasSize(2);
        assertThat(result.getProgresses().stream().filter(p -> p.getActivityId().equals(activityId)).findFirst().orElseThrow().isAchieved())
                .isEqualTo(achieved);
        assertThat(result.isCouponStackable()).isEqualTo(!achieved);
        if (!achieved) {
            assertThat(result.getNextThresholdAmount()).isEqualByComparingTo("120.00");
            service().reserve("below-threshold", result);
            verifyNoInteractions(reservationMapper);
            verify(activityMapper, never()).reserve(any(), any());
        }
    }

    @Test
    void recommendationSwitchDoesNotHideUnachievedProgress() {
        PromotionActivity activity = activity(2L, "FULL_DISCOUNT", 10);
        activity.setShowRecommendations(0);
        when(marketingFeatureService.isEnabled(2L, MarketingActivityCode.FULL_REDUCTION)).thenReturn(true);
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        stubPromotionDetails();

        PromotionCheckoutResult result = service().calculate(2L, items("110.88"));

        assertThat(result.getActivityId()).isEqualTo(2L);
        assertThat(result.getRemainingAmount()).isEqualByComparingTo("9.12");
        assertThat(result.getRecommendProductIds()).isEmpty();
    }

    @Test
    void activityWithoutThresholdsStillDoesNotProduceProgress() {
        when(marketingFeatureService.isEnabled(2L, MarketingActivityCode.FULL_REDUCTION)).thenReturn(true);
        when(activityMapper.selectList(any())).thenReturn(List.of(activity(2L, "FULL_DISCOUNT", 10)));
        when(scopeMapper.selectList(any())).thenReturn(List.of());
        when(thresholdMapper.selectList(any())).thenReturn(List.of());

        PromotionCheckoutResult result = service().calculate(2L, items("110.88"));

        assertThat(result.getActivityId()).isNull();
        assertThat(result.getProgresses()).isEmpty();
        assertThat(result.getRecommendProductIds()).isEmpty();
    }

    @Test
    void recalculatingBelowThresholdForRefundKeepsDiscountAtZero() {
        when(activityMapper.selectById(2L)).thenReturn(activity(2L, "FULL_DISCOUNT", 10));
        stubPromotionDetails();

        PromotionCheckoutResult result = service().calculateActivity(2L, items("110.88"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(result.getThresholdAmount()).isEqualByComparingTo("120.00");
        assertThat(result.getRemainingAmount()).isEqualByComparingTo("9.12");
        assertThat(result.getProgresses().get(0).isAchieved()).isFalse();
    }

    private PromotionService service() {
        return new PromotionServiceImpl(activityMapper, thresholdMapper, scopeMapper, reservationMapper, marketingFeatureService);
    }

    private List<PromotionPricingItem> items(String amount) {
        return List.of(new PromotionPricingItem(99L, 1L, new BigDecimal(amount)));
    }

    private PromotionActivity activity(Long id, String type, int priority) {
        PromotionActivity activity = new PromotionActivity();
        activity.setId(id);
        activity.setMerchantId(2L);
        activity.setName(type);
        activity.setActivityType(type);
        activity.setPriority(priority);
        activity.setScopeType(0);
        activity.setShowRecommendations(1);
        activity.setStackNewUserCoupon(0);
        activity.setStackRepurchaseCoupon(0);
        return activity;
    }

    private void stubPromotionDetails() {
        when(thresholdMapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<?> query = invocation.getArgument(0);
            query.getSqlSegment();
            return query.getParamNameValuePairs().containsValue(2L)
                    ? List.of(tier("500", null, "6", "120"), tier("200", null, "8", "20"), tier("120", null, "9", "8"))
                    : List.of(tier("139", "10", null, null), tier("119", "2", null, null));
        });
        when(scopeMapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<?> query = invocation.getArgument(0);
            query.getSqlSegment();
            if (!query.getParamNameValuePairs().containsValue(4)) return List.of();
            List<Long> ids = query.getParamNameValuePairs().containsValue(2L)
                    ? List.of(478L, 480L, 481L, 482L) : List.of(479L, 480L, 481L, 482L, 483L, 484L);
            return ids.stream().map(id -> {
                PromotionScope scope = new PromotionScope();
                scope.setTargetId(id);
                return scope;
            }).toList();
        });
    }

    private PromotionThreshold tier(String amount, String reduction, String rate, String cap) {
        PromotionThreshold threshold = new PromotionThreshold();
        threshold.setThresholdAmount(new BigDecimal(amount));
        if (reduction != null) threshold.setReductionAmount(new BigDecimal(reduction));
        if (rate != null) threshold.setDiscountRate(new BigDecimal(rate));
        if (cap != null) threshold.setDiscountCap(new BigDecimal(cap));
        return threshold;
    }

    @Test
    void updatePurgesReplacementOnlyChildrenBeforeSavingNewConfiguration() {
        PromotionActivity activity = new PromotionActivity();
        activity.setId(1L);
        activity.setMerchantId(2L);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        PromotionService service = new PromotionServiceImpl(activityMapper, thresholdMapper, scopeMapper, reservationMapper, marketingFeatureService);

        service.update(2L, 1L, request());

        verify(thresholdMapper).purgeByActivityId(1L);
        verify(scopeMapper).purgeByActivityId(1L);
    }

    private PromotionActivityRequest request() {
        PromotionThresholdRequest threshold = new PromotionThresholdRequest();
        threshold.setThresholdAmount(new BigDecimal("100.00"));
        threshold.setReductionAmount(new BigDecimal("10.00"));

        PromotionActivityRequest request = new PromotionActivityRequest();
        request.setName("满100减10");
        request.setActivityType("FULL_REDUCTION");
        request.setStatus(0);
        request.setScopeType(0);
        request.setStartAt(LocalDateTime.now());
        request.setEndAt(LocalDateTime.now().plusDays(1));
        request.setThresholds(List.of(threshold));
        return request;
    }
}
