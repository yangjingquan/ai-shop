package com.shop.marketing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionActivityVO {
    private Long id; private String name; private String activityType; private Integer priority; private Integer status;
    private LocalDateTime startAt; private LocalDateTime endAt; private Integer scopeType;
    private Integer stackNewUserCoupon; private Integer stackRepurchaseCoupon; private Integer showRecommendations;
    private BigDecimal budgetAmount; private Integer maxOrderCount; private BigDecimal reservedBudget; private Integer reservedOrderCount;
    private BigDecimal paidBudget; private Integer paidOrderCount;
    private List<Long> productIds; private List<Long> categoryIds; private List<Long> excludedProductIds; private List<Long> recommendProductIds;
    private List<PromotionThresholdRequest> thresholds;
}
