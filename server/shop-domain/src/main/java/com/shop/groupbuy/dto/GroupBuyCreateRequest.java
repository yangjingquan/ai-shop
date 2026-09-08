package com.shop.groupbuy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupBuyCreateRequest {
    @NotNull private Long productId;
    @NotNull private Long skuId;
    @NotNull @Min(1) private Integer quantity;
    @NotNull private Long addressId;
    @NotNull private String quoteId;
    @NotNull private Long ruleVersion;
    private String clientRequestId;
    private String remark;
}
