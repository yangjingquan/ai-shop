package com.shop.groupbuy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupBuyGroupStatus {
    WAIT_GROUP(0, "待成团"),
    FORMED(1, "已成团"),
    FAILED_WAIT_REFUND(2, "拼团失败/待退款"),
    FAILED_REFUNDED(3, "拼团失败/已退款"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String text;
}
