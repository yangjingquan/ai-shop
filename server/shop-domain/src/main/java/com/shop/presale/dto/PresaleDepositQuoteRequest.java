package com.shop.presale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PresaleDepositQuoteRequest {
    @NotNull private Long activityId;
    @NotNull private Long presaleSkuId;
    @NotNull private Long addressId;
    @NotNull @Min(1) private Integer quantity;
}
