package com.shop.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The only supported order-type dictionary.  Persist the numeric code for
 * backwards compatibility, but never make business decisions from literals.
 */
@Getter
@AllArgsConstructor
public enum OrderType {
    NORMAL(0, "普通订单"),
    GROUP_BUY(1, "拼团订单"),
    SECKILL(2, "秒杀订单"),
    POINTS_REDEEM(3, "积分兑换订单"),
    BUNDLE(4, "搭配购订单"),
    LOTTERY_PHYSICAL(5, "抽奖实物订单"),
    PRESALE(6, "预售订单");

    private final int code;
    private final String text;

    public static OrderType fromCode(Integer code) {
        if (code != null) {
            for (OrderType value : values()) {
                if (value.code == code) return value;
            }
        }
        return NORMAL;
    }
}
