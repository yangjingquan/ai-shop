package com.shop.presale.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PresaleSkuVO {
    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String mainImage;
    private String specText;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private BigDecimal depositAmount;
    private BigDecimal depositDeductionAmount;
    private BigDecimal balanceAmount;
    private BigDecimal finalAmount;
    private Integer userLimit;
    private Integer depositCount;
    private Integer balanceSoldCount;
    private Integer stock;
}
