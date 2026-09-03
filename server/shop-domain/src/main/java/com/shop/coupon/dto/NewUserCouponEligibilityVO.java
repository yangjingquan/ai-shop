package com.shop.coupon.dto;

import lombok.Data;

@Data
public class NewUserCouponEligibilityVO {
    private Boolean canReceive;
    private Boolean received;
    private CouponVO coupon;
}
