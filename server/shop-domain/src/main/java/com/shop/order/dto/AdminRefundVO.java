package com.shop.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRefundVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private String merchantName;
    private String reason;
    private Integer status;
    private String statusText;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
