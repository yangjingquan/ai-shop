package com.shop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartAddRequest {

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
