package com.shop.lottery.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDrawVO {
    private String drawNo;
    private LocalDateTime createdAt;
    private LotteryActivityVO activity;
    private LotteryPrizeVO prize;
    private LotteryRewardVO reward;
}
