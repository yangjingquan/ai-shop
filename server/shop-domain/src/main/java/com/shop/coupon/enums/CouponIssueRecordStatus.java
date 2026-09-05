package com.shop.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponIssueRecordStatus {
    PENDING(0), ISSUED(1), SKIPPED(2), REFUND_CANCELLED(3), REVOKED(4);

    private final int code;
}
