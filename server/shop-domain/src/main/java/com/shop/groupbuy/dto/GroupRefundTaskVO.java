package com.shop.groupbuy.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupRefundTaskVO {
    private Long id;
    private Long groupId;
    private String orderNo;
    private Long refundApplicationId;
    private String status;
    private String statusText;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
