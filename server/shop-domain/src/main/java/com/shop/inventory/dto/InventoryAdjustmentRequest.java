package com.shop.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InventoryAdjustmentRequest {

    @NotNull
    private Long skuId;

    /** 正数入库，负数出库。 */
    @NotNull
    @Min(-1000000)
    @Max(1000000)
    private Integer changeQty;

    @Size(max = 255)
    private String reason;
}
