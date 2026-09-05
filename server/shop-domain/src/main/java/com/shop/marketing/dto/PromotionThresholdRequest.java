package com.shop.marketing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionThresholdRequest {
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
    private BigDecimal thresholdAmount;
    @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
    private BigDecimal reductionAmount;
    @DecimalMin("0.01") @Digits(integer = 2, fraction = 2)
    private BigDecimal discountRate;
    @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
    private BigDecimal discountCap;
}
