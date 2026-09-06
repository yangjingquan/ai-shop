package com.shop.lottery.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryRewardVO {
    private Long id;
    private Long drawRecordId;
    private Long activityId;
    private String activityName;
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private String image;
    private Integer pointsAmount;
    private Long couponId;
    private String orderNo;
    private Integer status;
    private String statusText;
    private String failureReason;
    private LocalDateTime issuedAt;
    private LocalDateTime claimedAt;
    private LocalDateTime createdAt;
}
