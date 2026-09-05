package com.shop.coupon.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {
    private Long id;
    private Long templateId;
    private String name;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal thresholdAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer validityDays;
    private Integer status;
    private String statusText;
    private Boolean available;
    private String unavailableReason;
    private String issueScene;
    private String sourceOrderNo;
}
