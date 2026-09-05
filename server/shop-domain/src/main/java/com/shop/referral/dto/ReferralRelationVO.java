package com.shop.referral.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReferralRelationVO {
    private Long id;
    private Long inviterUserId;
    private Long inviteeUserId;
    private String firstOrderNo;
    private Integer status;
    private String statusText;
    private LocalDateTime boundAt;
    private LocalDateTime completedAt;
}
