package com.shop.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.marketing.entity.MerchantMarketingFeature;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.mapper.MerchantMarketingFeatureMapper;
import com.shop.marketing.service.impl.MarketingFeatureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingFeatureServiceImplTest {
    @Mock
    private MerchantMarketingFeatureMapper featureMapper;
    @Mock
    private StringRedisTemplate redisTemplate;

    private MarketingFeatureService service;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new MarketingFeatureServiceImpl(featureMapper, redisTemplate);
    }

    @Test
    void onlyImplementedAndEnabledFeaturesAreReturnedToMiniapp() {
        when(valueOperations.get("merchant:marketing:features:1")).thenReturn("GROUP_BUY,SECKILL");

        assertEquals(List.of("GROUP_BUY"), service.listEnabled(1L).stream()
                .map(item -> item.getCode()).toList());
        assertEquals(1, service.listEnabled(1L).get(0).getEnabled());
    }

    @Test
    void disabledFeatureIsRejectedEvenWhenMiniappCallsApiDirectly() {
        when(valueOperations.get("merchant:marketing:features:1")).thenReturn("");

        assertThrows(BusinessException.class,
                () -> service.assertEnabled(1L, MarketingActivityCode.GROUP_BUY));
    }

    @Test
    void cacheMissFallsBackToDatabase() {
        when(valueOperations.get("merchant:marketing:features:1")).thenReturn(null);
        MerchantMarketingFeature feature = new MerchantMarketingFeature();
        feature.setFeatureCode("GROUP_BUY");
        feature.setEnabled(1);
        when(featureMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(feature));

        service.assertEnabled(1L, MarketingActivityCode.GROUP_BUY);
    }

    @Test
    void databaseFailureFailsClosed() {
        when(valueOperations.get("merchant:marketing:features:1")).thenReturn(null);
        when(featureMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThrows(BusinessException.class,
                () -> service.assertEnabled(1L, MarketingActivityCode.GROUP_BUY));
    }
}
