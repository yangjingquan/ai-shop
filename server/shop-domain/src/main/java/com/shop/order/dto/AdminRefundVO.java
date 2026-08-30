package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminRefundVO {

    private Long id;
    private String orderNo;
    private String outRefundNo;
    private String wxRefundId;
    private Long userId;
    private Long merchantId;
    private String merchantName;
    private String reason;
    private Integer status;
    private String statusText;
    private String rejectReason;
    private BigDecimal refundAmount;
    private String refundFailReason;
    private LocalDateTime refundTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
