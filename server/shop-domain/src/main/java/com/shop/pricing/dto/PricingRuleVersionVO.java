package com.shop.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingRuleVersionVO {
    private Long version;
    private String matrixJson;
    private String zeroPayWhitelistJson;
    private BigDecimal maxDiscountRate;
}
