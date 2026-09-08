package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_task")
public class ReconciliationTask {
    @TableId(type = IdType.AUTO) private Long id;
    private String taskNo;
    private String taskType;
    private Long merchantId;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private String status;
    private String idempotencyKey;
    private String summaryJson;
    private String errorMessage;
    private String requestedBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
