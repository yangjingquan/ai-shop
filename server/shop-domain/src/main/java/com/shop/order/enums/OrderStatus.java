package com.shop.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    WAIT_PAY(0, "待支付"),
    WAIT_SHIP(1, "待发货"),
    WAIT_RECEIVE(2, "待收货"),
    FINISHED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String text;

    public static String statusText(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s.text;
        }
        return "未知";
    }

    public static boolean canCancel(int code) {
        return code == WAIT_PAY.code;
    }
}
