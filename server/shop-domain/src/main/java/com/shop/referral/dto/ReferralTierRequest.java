package com.shop.referral.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReferralTierRequest {
    @NotNull @Min(1)
    private Integer inviteCount;
    @NotNull
    private Long inviterCouponTemplateId;
}
