package com.shop.referral.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReferralBindRequest {
    @NotBlank
    private String token;
}
