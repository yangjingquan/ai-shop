package com.shop.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("referral_share_token")
public class ReferralShareToken extends BaseEntity {
    private Long campaignId;
    private Long merchantId;
    private Long inviterUserId;
    private String token;
    private LocalDateTime expiresAt;
}
