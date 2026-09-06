package com.shop.lottery.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LotteryPrizeVO {
    private Long id;
    private String name;
    private String prizeType;
    private String image;
    private Integer pointsAmount;
    private Long couponTemplateId;
    private Long productId;
    private Long skuId;
    private Integer totalStock;
    private Integer remainingStock;
    private BigDecimal probability;
    private Integer validityDays;
    private Integer sort;
    private Integer status;
}
