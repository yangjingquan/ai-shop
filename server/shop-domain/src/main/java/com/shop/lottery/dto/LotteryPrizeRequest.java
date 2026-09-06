package com.shop.lottery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotteryPrizeRequest {
    private Long id;
    @NotBlank private String name;
    @NotBlank private String prizeType;
    private String image;
    @Min(0) private Integer pointsAmount;
    private Long couponTemplateId;
    private Long productId;
    private Long skuId;
    @Min(0) private Integer totalStock = 0;
    @NotNull @DecimalMin("0.00000001") @DecimalMax("1.00000000") private BigDecimal probability;
    @Min(0) private Integer validityDays = 0;
    private Integer sort = 0;
    private Integer status = 1;
}
