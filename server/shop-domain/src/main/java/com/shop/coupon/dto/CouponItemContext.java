package com.shop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CouponItemContext {
    private Long productId;
    private Long categoryId;
    private BigDecimal subtotal;
    private boolean activityGoods;
}
