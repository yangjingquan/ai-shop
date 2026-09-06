package com.shop.lottery.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LotteryStatsVO {
    private long participants;
    private long drawCount;
    private long rewardsIssued;
    private long rewardsPending;
    private long couponRewards;
    private long physicalRewards;
    private long pointsRewards;
    private BigDecimal pointsCost = BigDecimal.ZERO;
}
