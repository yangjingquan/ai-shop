package com.shop.pricing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("price_quote")
public class PriceQuote {
    @TableId
    private String id;
    private Long userId;
    private Long merchantId;
    private String scene;
    private Long ruleVersion;
    private BigDecimal originalAmount;
    private BigDecimal activityDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal pointsDiscountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payableAmount;
    private Long couponId;
    private String snapshotJson;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
