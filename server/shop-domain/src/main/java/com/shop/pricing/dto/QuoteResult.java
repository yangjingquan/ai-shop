package com.shop.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuoteResult {
    private String quoteId;
    private Long ruleVersion;
    private String scene;
    private BigDecimal originalAmount;
    private BigDecimal activityDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal pointsDiscountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payableAmount;
    private List<String> unavailableReasons = List.of();
    private LocalDateTime expiresAt;
    private String pricingSnapshotJson;
}
