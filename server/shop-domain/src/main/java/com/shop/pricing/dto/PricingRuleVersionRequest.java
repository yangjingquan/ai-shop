package com.shop.pricing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PricingRuleVersionRequest {
    @NotBlank private String matrixJson;
    @NotBlank private String zeroPayWhitelistJson;
    @DecimalMin("0.0000") @DecimalMax("1.0000") private BigDecimal maxDiscountRate;
    private String reason;
}
