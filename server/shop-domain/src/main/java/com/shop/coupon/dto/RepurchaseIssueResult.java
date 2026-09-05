package com.shop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RepurchaseIssueResult {
    private final Long couponId;
    private final boolean issued;
}
