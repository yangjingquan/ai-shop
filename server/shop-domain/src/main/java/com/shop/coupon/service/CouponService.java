package com.shop.coupon.service;

import com.shop.coupon.dto.*;

import java.util.List;

public interface CouponService {
    void initializeMerchant(Long merchantId);

    List<CouponTemplateVO> listTemplates(Long merchantId);

    Long createTemplate(Long merchantId, CouponTemplateSaveRequest request);

    void updateTemplate(Long merchantId, Long templateId, CouponTemplateSaveRequest request);

    void updateTemplateStatus(Long merchantId, Long templateId, Integer status);

    NewUserCouponEligibilityVO eligibility(Long userId, Long merchantId);

    Long receiveNewUserCoupon(Long userId, Long merchantId, Long templateId);

    List<CouponVO> listUserCoupons(Long userId, Long merchantId, Integer status);

    CouponCheckoutResult calculate(Long userId, CouponUseContext context, Long requestedCouponId,
                                   boolean consume, String orderNo);

    void releaseBeforePaymentCancel(Long orderId, String orderNo);
}
