package com.shop.referral.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReferralRelationStatus {
    BOUND(0, "待首购"), COMPLETED(1, "已完成"), FROZEN(2, "已冻结"), REFUND_PENDING(3, "退款待处理"), INVALID(4, "已失效");

    private final int code;
    private final String text;
}
