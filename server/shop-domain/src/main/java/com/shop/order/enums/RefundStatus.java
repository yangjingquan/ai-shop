package com.shop.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatus {
    PENDING(0, "待处理"),
    APPROVED(1, "已同意"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String text;
}
