package com.shop.coupon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponTemplateSaveRequest {
    @NotBlank
    private String name;

    /** 券面展示图，供积分商城等营销入口使用。 */
    private String image;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal thresholdAmount;

    /** 0 表示不限库存。 */
    @NotNull
    @Min(0)
    private Integer totalStock;

    @NotNull
    @Min(1)
    private Integer perUserLimit = 1;

    @NotNull
    @Min(1)
    private Integer validityDays = 30;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer scopeType = 0;
    private List<Long> scopeIds;
    private Integer newUserOnly = 1;
    private String issueScene = "NEW_USER";
    private Integer repurchaseTargetType = 0;
    private List<Long> repurchaseTargetIds;
    @DecimalMin("0.00")
    private BigDecimal repurchaseMinOrderAmount = BigDecimal.ZERO;
    private Integer repurchaseFirstPurchaseOnly = 0;
    private Integer repurchasePriority = 0;
    private Integer excludeActivityGoods = 1;
    private Integer stackable = 0;
    private Integer status = 1;
}
