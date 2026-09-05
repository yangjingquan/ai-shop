package com.shop.referral.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReferralCampaignSaveRequest {
    @NotBlank private String name;
    @NotBlank private String shareTitle;
    private String shareDescription;
    private Long landingProductId;
    @NotNull private Long inviteeCouponTemplateId;
    @Valid @NotEmpty private List<ReferralTierRequest> tiers;
    @NotNull private LocalDateTime startAt;
    @NotNull private LocalDateTime endAt;
    private Integer maxDailyInvites = 20;
    private Integer maxTotalInvites = 0;
    private Integer status = 0;
}
