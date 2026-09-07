package com.shop.seckill.dto;

import com.shop.order.dto.AddressSnapshot;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillOrderPreviewVO {
    private AddressSnapshot address;
    private Long sessionId;
    private Long seckillSkuId;
    private String productName;
    private String mainImage;
    private String specText;
    private BigDecimal activityPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String quoteId;
    private Long ruleVersion;
    private BigDecimal originalAmount;
    private BigDecimal activityDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal pointsDiscountAmount;
    private java.time.LocalDateTime quoteExpiresAt;
    private Integer userLimit;
    private Integer remainingStock;
    private String ruleText;
}
