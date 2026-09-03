package com.shop.groupbuy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_refund_task")
public class GroupRefundTask extends BaseEntity {
    private Long merchantId;
    private Long groupId;
    private String orderNo;
    private Long refundApplicationId;
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime completedAt;
}
