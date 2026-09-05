package com.shop.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("referral_campaign")
public class ReferralCampaign extends BaseEntity {
    private Long merchantId;
    private String name;
    private String shareTitle;
    private String shareDescription;
    private Long landingProductId;
    private Long inviteeCouponTemplateId;
    private String tierConfigJson;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer maxDailyInvites;
    private Integer maxTotalInvites;
    private Integer status;
}
