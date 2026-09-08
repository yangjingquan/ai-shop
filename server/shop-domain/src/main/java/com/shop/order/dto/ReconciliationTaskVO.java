package com.shop.order.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReconciliationTaskVO {
    private String taskNo;
    private String taskType;
    private Long merchantId;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private String status;
    private Integer affectedCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
