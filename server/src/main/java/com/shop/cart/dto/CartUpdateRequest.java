package com.shop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改购物车数量；quantity = 0 视为删除该项。
 */
@Data
public class CartUpdateRequest {

    @NotNull
    @Min(0)
    private Integer quantity;
}
