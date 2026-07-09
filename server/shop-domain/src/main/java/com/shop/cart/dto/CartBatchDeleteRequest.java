package com.shop.cart.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CartBatchDeleteRequest {

    @NotEmpty(message = "购物车项不能为空")
    private List<Long> ids;
}
