package com.shop.referral.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReferralCampaignVO {
    private Long id;
    private String name;
    private String shareTitle;
    private String shareDescription;
    private Long landingProductId;
    private String landingProductName;
    private String landingProductImage;
    private String landingProductPrice;
    private Long inviteeCouponTemplateId;
    private String inviteeCouponName;
    private String inviteeCouponAmount;
    private List<ReferralTierVO> tiers;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer maxDailyInvites;
    private Integer maxTotalInvites;
    private Integer status;
    private String statusText;
    private Boolean active;
    private String shareToken;
    private Integer completedInviteCount;
    private Integer pendingInviteCount;
    private Integer nextTierInviteCount;
    private Integer remainingToNextTier;
    private Boolean invitee;
    private Boolean oldUser;
    private Boolean canClaimInviteeCoupon;
    private Long inviteeCouponId;
}
