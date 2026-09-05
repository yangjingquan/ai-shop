package com.shop.marketing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionActivityRequest {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Pattern(regexp = "FULL_REDUCTION|FULL_DISCOUNT") private String activityType;
    private Integer priority = 0;
    @NotNull @Min(0) @Max(2) private Integer status;
    @NotNull private LocalDateTime startAt;
    @NotNull private LocalDateTime endAt;
    @NotNull @Min(0) @Max(2) private Integer scopeType;
    private Integer stackNewUserCoupon = 0;
    private Integer stackRepurchaseCoupon = 0;
    private Integer showRecommendations = 0;
    @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) private BigDecimal budgetAmount;
    @Min(1) private Integer maxOrderCount;
    private List<Long> productIds = List.of();
    private List<Long> categoryIds = List.of();
    private List<Long> excludedProductIds = List.of();
    private List<Long> recommendProductIds = List.of();
    @NotEmpty private List<@Valid PromotionThresholdRequest> thresholds;
}
