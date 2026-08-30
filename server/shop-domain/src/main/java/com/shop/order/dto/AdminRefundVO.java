package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> evidenceUrls;
    private Integer status;
    private String statusText;
    private String rejectReason;
    private BigDecimal refundAmount;
    private String refundFailReason;
    private LocalDateTime refundTime;
    private Integer autoRefund;
    private Integer returnRequired;
    private String returnShipCompany;
    private String returnShipNo;
    private LocalDateTime returnShipTime;
    private LocalDateTime returnReceivedTime;
    private String returnReceiveNote;
    private LocalDateTime refundReconcileAt;
    private Integer refundReconcileAttempts;
    private String refundReconcileError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
