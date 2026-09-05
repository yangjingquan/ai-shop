package com.shop.marketing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionCheckoutResult {
    private Long activityId;
    private String activityName;
    private String activityType;
    private BigDecimal qualifiedAmount = BigDecimal.ZERO;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal nextThresholdAmount;
    private BigDecimal remainingAmount = BigDecimal.ZERO;
    private boolean couponStackable;
    private List<Long> recommendProductIds = List.of();
    private List<PromotionProgress> progresses = List.of();

    @Data
    public static class PromotionProgress {
        private Long activityId; private String activityName; private String activityType;
        private BigDecimal qualifiedAmount; private BigDecimal thresholdAmount; private BigDecimal discountAmount;
        private BigDecimal nextThresholdAmount; private BigDecimal remainingAmount; private boolean achieved;
    }
}
