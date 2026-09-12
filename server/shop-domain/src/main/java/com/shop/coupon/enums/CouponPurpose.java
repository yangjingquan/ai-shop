package com.shop.coupon.enums;

/** 优惠券模板的业务用途；发放场景描述何时发，用途描述由谁发、发给谁。 */
public final class CouponPurpose {
    private CouponPurpose() {}

    public static final String NEW_USER_FIRST_ORDER = "NEW_USER_FIRST_ORDER";
    public static final String REFERRAL_INVITEE = "REFERRAL_INVITEE";
    public static final String REFERRAL_INVITER = "REFERRAL_INVITER";
    public static final String MARKETING_REWARD = "MARKETING_REWARD";

    public static boolean isSupported(String value) {
        return NEW_USER_FIRST_ORDER.equals(value)
                || REFERRAL_INVITEE.equals(value)
                || REFERRAL_INVITER.equals(value)
                || MARKETING_REWARD.equals(value);
    }
}
