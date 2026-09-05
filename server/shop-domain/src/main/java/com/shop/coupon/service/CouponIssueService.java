package com.shop.coupon.service;

import com.shop.coupon.dto.RepurchaseCouponVO;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;

/** 订单触发型优惠券的幂等发放与退款回收。 */
public interface CouponIssueService {
    void issueAfterPaid(Order order);

    void revokeAfterFullRefund(Order order, RefundApplication refund);

    RepurchaseCouponVO findRepurchaseCoupon(Long userId, Long merchantId, String orderNo);
}
