package com.shop.bundle.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BundleSkuVO {
    private Long id;
    private String specText;
    private BigDecimal price;
    private Integer stock;
    private String image;
}
