package com.shop.coupon.service;

import com.shop.coupon.dto.*;

import java.util.List;

public interface CouponService {
    void initializeMerchant(Long merchantId);

    /**
     * 按发放场景返回模板。运营端必须显式在各自活动入口管理，避免复购券被旧活动误选。
     */
    List<CouponTemplateVO> listTemplates(Long merchantId, String issueScene);

    Long createTemplate(Long merchantId, CouponTemplateSaveRequest request);

    void updateTemplate(Long merchantId, Long templateId, CouponTemplateSaveRequest request);

    void updateTemplateStatus(Long merchantId, Long templateId, Integer status);

    NewUserCouponEligibilityVO eligibility(Long userId, Long merchantId);

    Long receiveNewUserCoupon(Long userId, Long merchantId, Long templateId);

    /** 按通用营销奖励模板向指定用户发券，供积分、会员日、抽奖等服务端奖励使用。 */
    Long issueTemplate(Long userId, Long merchantId, Long templateId);

    /** 按指定业务用途发券，防止不同营销活动误用同一模板。 */
    Long issueTemplateForPurpose(Long userId, Long merchantId, Long templateId, String purpose);

    /** 积分兑换专用发券；达到模板每人领取上限时必须失败，避免扣积分但不发新券。 */
    Long issueTemplateForPoints(Long userId, Long merchantId, Long templateId);

    RepurchaseIssueResult issueRepurchaseCoupon(Long userId, Long merchantId, Long templateId, String sourceOrderNo);

    /** 撤销一张尚未使用的用户券；已使用券不做逆向扣减。 */
    boolean invalidateCoupon(Long userId, Long merchantId, Long couponId);

    boolean invalidateCoupon(Long userId, Long merchantId, Long couponId, String reason);

    List<CouponVO> listUserCoupons(Long userId, Long merchantId, Integer status);

    CouponCheckoutResult calculate(Long userId, CouponUseContext context, Long requestedCouponId,
                                   boolean consume, String orderNo);

    void releaseBeforePaymentCancel(Long orderId, String orderNo);
}
