package com.shop.referral.dto;

import lombok.Data;

@Data
public class ReferralTierVO {
    private Integer inviteCount;
    private Long inviterCouponTemplateId;
    private String couponName;
    private String couponAmount;
    private Boolean reached;
    private Boolean rewarded;
}
