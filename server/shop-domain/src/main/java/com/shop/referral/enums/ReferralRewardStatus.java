package com.shop.referral.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReferralRewardStatus {
    PENDING(0, "待发放"), ISSUED(1, "已发券"), REVOKED(2, "已撤销"), FAILED(3, "发放失败"), FROZEN(4, "已冻结");

    private final int code;
    private final String text;
}
