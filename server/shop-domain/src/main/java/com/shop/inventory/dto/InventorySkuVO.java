package com.shop.inventory.dto;

import lombok.Data;

@Data
public class InventorySkuVO {
    private Long skuId;
    private Long productId;
    private String productName;
    private String mainImage;
    private String skuCode;
    private String specText;
    private Integer stock;
}
