package com.shop.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillProductVO {
    private Long seckillSkuId;
    private Long productId;
    private Long skuId;
    private String productName;
    private String mainImage;
    private String specText;
    private BigDecimal activityPrice;
    private BigDecimal originalPrice;
    private Integer activityStock;
    private Integer soldCount;
    private Integer remainingStock;
    private Integer userLimit;
    private Integer status;
    private String statusText;
}
