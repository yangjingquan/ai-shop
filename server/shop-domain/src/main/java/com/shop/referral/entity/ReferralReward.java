package com.shop.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("referral_reward")
public class ReferralReward extends BaseEntity {
    private Long campaignId;
    private Long merchantId;
    private Long relationId;
    private Long userId;
    private String role;
    private Integer tier;
    private Long couponTemplateId;
    private Long couponId;
    private BigDecimal rewardAmount;
    private String triggerOrderNo;
    private Integer status;
    private String failureReason;
    private String revokeReason;
    private LocalDateTime issuedAt;
    private LocalDateTime revokedAt;
}
