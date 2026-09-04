package com.shop.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillOrderPreviewRequest {
    @NotNull
    private Long sessionId;
    @NotNull
    private Long seckillSkuId;
    @NotNull
    private Long addressId;
    @Min(1)
    private Integer quantity = 1;
}
