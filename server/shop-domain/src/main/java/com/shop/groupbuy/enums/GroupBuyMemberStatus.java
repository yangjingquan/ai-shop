package com.shop.groupbuy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupBuyMemberStatus {
    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    WAIT_REFUND(3, "待退款");

    private final int code;
    private final String text;
}
