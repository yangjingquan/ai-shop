package com.shop.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponTemplateType {
    FULL_REDUCTION(1, "满减券");

    private final int code;
    private final String text;
}
