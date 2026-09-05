package com.shop.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.coupon.dto.CouponCheckoutResult;
import com.shop.coupon.dto.CouponItemContext;
import com.shop.coupon.dto.CouponTemplateSaveRequest;
import com.shop.coupon.dto.CouponUseContext;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.entity.UserCoupon;
import com.shop.coupon.enums.CouponIssueScene;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.coupon.enums.UserCouponStatus;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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

    @Test
    void issuesRepurchaseCouponOnceAndStoresSourceOrder() {
        CouponServiceImpl service = new CouponServiceImpl(templateMapper, userCouponMapper, orderMapper, marketingFeatureService);
        CouponTemplate template = new CouponTemplate();
        template.setId(101L); template.setMerchantId(9L); template.setName("复购券");
        template.setIssueScene(CouponIssueScene.REPURCHASE_AFTER_PAID); template.setStatus(1);
        template.setAmount(new BigDecimal("15")); template.setThresholdAmount(new BigDecimal("99"));
        template.setScopeType(0); template.setExcludeActivityGoods(1); template.setValidityDays(7);
        template.setPerUserLimit(1); template.setTotalStock(0); template.setReceivedCount(0); template.setUsedCount(0);
        when(templateMapper.selectOne(any())).thenReturn(template);
        when(userCouponMapper.selectCount(any())).thenReturn(0L);

        var result = service.issueRepurchaseCoupon(7L, 9L, 101L, "ORDER-1");

        assertTrue(result.isIssued());
        ArgumentCaptor<UserCoupon> couponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(couponCaptor.capture());
        assertEquals(CouponIssueScene.REPURCHASE_AFTER_PAID, couponCaptor.getValue().getIssueScene());
        assertEquals("ORDER-1", couponCaptor.getValue().getSourceOrderNo());
    }

    @Test
    void exposesFullRefundReasonForInvalidCoupon() {
        CouponServiceImpl service = new CouponServiceImpl(templateMapper, userCouponMapper, orderMapper, marketingFeatureService);
        UserCoupon coupon = coupon(4L, new BigDecimal("15"), new BigDecimal("99"), 0);
        coupon.setStatus(UserCouponStatus.INVALID.getCode());
        coupon.setInvalidReason("原订单已全额退款，优惠券已失效");
        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));

        var coupons = service.listUserCoupons(7L, 9L, UserCouponStatus.EXPIRED.getCode());

        assertEquals(1, coupons.size());
        assertEquals("原订单已全额退款，优惠券已失效", coupons.get(0).getUnavailableReason());
    }

    @Test
    void rejectsChangingTemplateIssueSceneAfterCreation() {
        CouponServiceImpl service = new CouponServiceImpl(templateMapper, userCouponMapper, orderMapper, marketingFeatureService);
        CouponTemplate template = new CouponTemplate();
        template.setId(101L); template.setMerchantId(9L); template.setIssueScene(CouponIssueScene.NEW_USER);
        when(templateMapper.selectOne(any())).thenReturn(template);
        CouponTemplateSaveRequest request = templateRequest(CouponIssueScene.REPURCHASE_AFTER_PAID);

        var error = assertThrows(RuntimeException.class, () -> service.updateTemplate(9L, 101L, request));

        assertTrue(error.getMessage().contains("不能变更发放场景"));
        verify(templateMapper, never()).updateById(any());
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

    private CouponTemplateSaveRequest templateRequest(String issueScene) {
        CouponTemplateSaveRequest request = new CouponTemplateSaveRequest();
        request.setName("测试券"); request.setAmount(new BigDecimal("10")); request.setThresholdAmount(new BigDecimal("50"));
        request.setTotalStock(0); request.setPerUserLimit(1); request.setValidityDays(7); request.setScopeType(0);
        request.setNewUserOnly(0); request.setIssueScene(issueScene); request.setStatus(1);
        request.setRepurchaseTargetType(0); request.setRepurchaseFirstPurchaseOnly(0);
        return request;
    }
}
