package com.shop.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserCouponStatus {
    WAIT_USE(0, "可使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期"),
    INVALID(3, "已失效");

    private final int code;
    private final String text;
}
