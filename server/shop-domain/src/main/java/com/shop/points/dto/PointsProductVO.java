package com.shop.points.dto;
import lombok.Data;
@Data public class PointsProductVO { private Long id; private Long productId; private Long skuId; private Long couponTemplateId; private String title; private String image; private Integer pointsPrice; private Integer stock; private Integer perUserLimit; private Integer redeemedCount; private Integer status; private boolean physical; }
