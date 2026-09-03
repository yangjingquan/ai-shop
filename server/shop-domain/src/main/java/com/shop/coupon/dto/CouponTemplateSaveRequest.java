package com.shop.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponTemplateSaveRequest {
    @NotBlank
    private String name;

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
    private Integer excludeActivityGoods = 1;
    private Integer stackable = 0;
    private Integer status = 1;
}
