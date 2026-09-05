package com.shop.marketing.service;

import com.shop.marketing.dto.PromotionActivityRequest;
import com.shop.marketing.dto.PromotionThresholdRequest;
import com.shop.marketing.entity.PromotionActivity;
import com.shop.marketing.mapper.PromotionActivityMapper;
import com.shop.marketing.mapper.PromotionOrderReservationMapper;
import com.shop.marketing.mapper.PromotionScopeMapper;
import com.shop.marketing.mapper.PromotionThresholdMapper;
import com.shop.marketing.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {
    @Mock private PromotionActivityMapper activityMapper;
    @Mock private PromotionThresholdMapper thresholdMapper;
    @Mock private PromotionScopeMapper scopeMapper;
    @Mock private PromotionOrderReservationMapper reservationMapper;
    @Mock private MarketingFeatureService marketingFeatureService;

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
