package com.shop.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundItemRequest {
    @NotNull private Long orderItemId;
    @NotNull @Min(1) private Integer quantity;
}
