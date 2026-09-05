package com.shop.referral.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReferralRewardVO {
    private Long id;
    private Long relationId;
    private Long userId;
    private String role;
    private Integer tier;
    private Long couponTemplateId;
    private Long couponId;
    private BigDecimal rewardAmount;
    private String triggerOrderNo;
    private Integer status;
    private String statusText;
    private String failureReason;
    private String revokeReason;
    private LocalDateTime issuedAt;
    private LocalDateTime revokedAt;
}
