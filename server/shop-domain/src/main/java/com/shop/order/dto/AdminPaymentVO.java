package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminPaymentVO {
    private Long id;
    private String orderNo;
    private String transactionId;
    private BigDecimal amount;
    private Long merchantId;
    private String merchantName;
    private Integer orderStatus;
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
    private LocalDateTime payReconcileAt;
    private Integer payReconcileAttempts;
    private String payReconcileError;
}
