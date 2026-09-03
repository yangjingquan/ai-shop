package com.shop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CouponUseContext {
    private Long merchantId;
    private BigDecimal goodsAmount;
    private List<CouponItemContext> items;
}
