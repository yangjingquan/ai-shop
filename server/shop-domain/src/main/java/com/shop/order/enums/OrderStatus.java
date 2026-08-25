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
    CANCELLED(4, "已取消"),
    WAIT_GROUP(5, "待成团"),
    GROUP_SUCCESS(6, "已成团"),
    GROUP_FAILED_WAIT_REFUND(7, "拼团失败/待退款");

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
