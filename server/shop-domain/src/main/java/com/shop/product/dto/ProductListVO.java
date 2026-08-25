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

    private BigDecimal minOriginalPrice;

    private BigDecimal maxOriginalPrice;

    private Integer totalStock;

    private Integer totalSales;

    private Integer status;

    private Integer auditStatus;

    private String auditReason;

    private Long auditedBy;

    private java.time.LocalDateTime auditedAt;

    private Integer isRecommend;

    private Integer isGroupBuy;

    private BigDecimal groupBuyPrice;

    private Integer groupBuyRequiredCount;

    private Long categoryId;

    private String categoryName;
}
