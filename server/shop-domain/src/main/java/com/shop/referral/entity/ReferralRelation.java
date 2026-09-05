package com.shop.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("referral_relation")
public class ReferralRelation extends BaseEntity {
    private Long campaignId;
    private Long merchantId;
    private Long inviterUserId;
    private Long inviteeUserId;
    private String sourceToken;
    private String firstOrderNo;
    private Integer status;
    private LocalDateTime boundAt;
    private LocalDateTime completedAt;
}
