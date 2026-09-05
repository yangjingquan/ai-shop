package com.shop.marketing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class PromotionPricingItem {
    private Long productId;
    private Long categoryId;
    private BigDecimal subtotal;
}
