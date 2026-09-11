package com.shop.product.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductSortRequest {

    /** 当前列表中商品的拖动后顺序。 */
    @NotEmpty
    private List<@NotNull Long> productIds;
}
