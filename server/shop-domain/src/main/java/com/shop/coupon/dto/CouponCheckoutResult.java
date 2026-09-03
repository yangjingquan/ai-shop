package com.shop.coupon.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CouponCheckoutResult {
    private Long selectedCouponId;
    private Long selectedCouponTemplateId;
    private String selectedCouponName;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String unavailableReason;
    private List<CouponVO> coupons;
}
