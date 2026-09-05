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

    /** 按模板向指定用户发券，供邀请奖励等服务端奖励使用。 */
    Long issueTemplate(Long userId, Long merchantId, Long templateId);

    /** 撤销一张尚未使用的用户券；已使用券不做逆向扣减。 */
    boolean invalidateCoupon(Long userId, Long merchantId, Long couponId);

    List<CouponVO> listUserCoupons(Long userId, Long merchantId, Integer status);

    CouponCheckoutResult calculate(Long userId, CouponUseContext context, Long requestedCouponId,
                                   boolean consume, String orderNo);

    void releaseBeforePaymentCancel(Long orderId, String orderNo);
}
