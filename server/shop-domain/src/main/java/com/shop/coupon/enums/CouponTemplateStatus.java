package com.shop.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponTemplateStatus {
    DRAFT(0, "草稿"),
    ACTIVE(1, "进行中"),
    STOPPED(2, "已停止");

    private final int code;
    private final String text;
}
