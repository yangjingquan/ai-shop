package com.shop.product.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductListVO {

    private Long id;

    private Long merchantId;

    private String name;

    private String mainImage;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer totalStock;

    private Integer totalSales;

    private Integer status;

    private Long categoryId;

    private String categoryName;
}
