package com.shop.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.coupon.dto.CouponCheckoutResult;
import com.shop.coupon.dto.CouponItemContext;
import com.shop.coupon.dto.CouponUseContext;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.entity.UserCoupon;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.coupon.enums.UserCouponStatus;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {
    @Mock private CouponTemplateMapper templateMapper;
    @Mock private UserCouponMapper userCouponMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private MarketingFeatureService marketingFeatureService;

    @Test
    void excludesActivityGoodsBeforeApplyingThresholdAndDiscount() {
        CouponServiceImpl service = new CouponServiceImpl(templateMapper, userCouponMapper, orderMapper, marketingFeatureService);
        UserCoupon coupon = coupon(1L, new BigDecimal("20"), new BigDecimal("50"), 1);
        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));

        CouponCheckoutResult result = service.calculate(7L,
                new CouponUseContext(9L, new BigDecimal("120"), List.of(
                        new CouponItemContext(11L, 2L, new BigDecimal("40"), true),
                        new CouponItemContext(12L, 2L, new BigDecimal("80"), false))),
                1L, false, null);

        assertEquals(new BigDecimal("20"), result.getDiscountAmount());
        assertEquals(1L, result.getSelectedCouponId());
        assertTrue(result.getCoupons().get(0).getAvailable());
    }

    @Test
    void returnsThresholdReasonAndNeverDiscountsWhenEligibleAmountIsInsufficient() {
        CouponServiceImpl service = new CouponServiceImpl(templateMapper, userCouponMapper, orderMapper, marketingFeatureService);
        UserCoupon coupon = coupon(2L, new BigDecimal("20"), new BigDecimal("99"), 0);
        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));

        CouponCheckoutResult result = service.calculate(7L,
                new CouponUseContext(9L, new BigDecimal("120"), List.of(
                        new CouponItemContext(12L, 2L, new BigDecimal("80"), false))),
                2L, false, null);

        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
        assertEquals("未满足满99元使用门槛", result.getUnavailableReason());
        assertFalse(result.getCoupons().get(0).getAvailable());
    }

    @Test
    void consumesCouponOnlyAfterServerSideRecheck() {
        CouponServiceImpl service = new CouponServiceImpl(templateMapper, userCouponMapper, orderMapper, marketingFeatureService);
        UserCoupon coupon = coupon(3L, new BigDecimal("20"), new BigDecimal("50"), 0);
        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));
        when(userCouponMapper.selectOne(any())).thenReturn(coupon);

        CouponCheckoutResult result = service.calculate(7L,
                new CouponUseContext(9L, new BigDecimal("80"), List.of(
                        new CouponItemContext(12L, 2L, new BigDecimal("80"), false))),
                3L, true, "ORDER-1");

        assertEquals(UserCouponStatus.USED.getCode(), coupon.getStatus());
        assertEquals("ORDER-1", coupon.getUsedOrderNo());
        assertEquals(new BigDecimal("20"), result.getDiscountAmount());
        verify(userCouponMapper).updateById(coupon);
    }

    private UserCoupon coupon(Long id, BigDecimal amount, BigDecimal threshold, int excludeActivityGoods) {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(id); coupon.setTemplateId(id + 100); coupon.setMerchantId(9L);
        coupon.setTemplateNameSnapshot("新人首单券"); coupon.setType(1); coupon.setAmountSnapshot(amount);
        coupon.setThresholdSnapshot(threshold); coupon.setScopeTypeSnapshot(0);
        coupon.setExcludeActivityGoodsSnapshot(excludeActivityGoods); coupon.setStatus(0);
        coupon.setValidFrom(LocalDateTime.now().minusDays(1)); coupon.setValidTo(LocalDateTime.now().plusDays(1));
        return coupon;
    }
}
