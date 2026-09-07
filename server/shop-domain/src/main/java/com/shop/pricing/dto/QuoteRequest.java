package com.shop.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuoteRequest {
    private Long userId;
    private Long merchantId;
    /** NORMAL / SECKILL / GROUP_BUY / PRESALE_DEPOSIT / PRESALE_BALANCE / BUNDLE / POINTS_EXCHANGE / LOTTERY_PHYSICAL */
    private String scene;
    private BigDecimal originalAmount;
    private BigDecimal activityDiscountAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;
    /** I0-01 only calculates this field. I0-03 owns the actual points reservation. */
    private BigDecimal pointsDiscountAmount = BigDecimal.ZERO;
    private BigDecimal freightAmount = BigDecimal.ZERO;
    private Long couponId;
    private String activityName;
    private List<QuoteItem> items = List.of();

    @Data
    public static class QuoteItem {
        private Long productId;
        private Long skuId;
        private Integer quantity;
        private BigDecimal originalUnitPrice;
        private BigDecimal activityUnitPrice;
    }
}
