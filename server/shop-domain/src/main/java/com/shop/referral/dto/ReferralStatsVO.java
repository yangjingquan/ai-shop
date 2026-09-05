package com.shop.referral.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReferralStatsVO {
    private long shares;
    private long opens;
    private long registrations;
    private long firstPurchases;
    private long rewardsIssued;
    private BigDecimal rewardCost = BigDecimal.ZERO;
}
