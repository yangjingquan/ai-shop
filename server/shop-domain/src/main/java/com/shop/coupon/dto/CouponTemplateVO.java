package com.shop.coupon.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponTemplateVO {
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal thresholdAmount;
    private Integer totalStock;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer perUserLimit;
    private Integer validityDays;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer scopeType;
    private List<Long> scopeIds;
    private Integer newUserOnly;
    private Integer excludeActivityGoods;
    private Integer stackable;
    private Integer status;
    private String statusText;
}
