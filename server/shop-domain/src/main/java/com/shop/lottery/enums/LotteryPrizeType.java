package com.shop.lottery.enums;

public enum LotteryPrizeType {
    POINTS, COUPON, PHYSICAL, CONSOLATION;

    public static boolean valid(String value) {
        for (LotteryPrizeType type : values()) if (type.name().equals(value)) return true;
        return false;
    }
}
