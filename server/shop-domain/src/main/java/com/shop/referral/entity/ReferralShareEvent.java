package com.shop.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("share_event")
public class ReferralShareEvent extends BaseEntity {
    private Long campaignId;
    private Long merchantId;
    private Long relationId;
    private Long inviterUserId;
    private Long inviteeUserId;
    private String token;
    private String eventType;
    private String orderNo;
}
