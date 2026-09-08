package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("after_sales_intervention")
public class AfterSalesIntervention {
    @TableId(type = IdType.AUTO) private Long id;
    private Long refundId;
    private String orderNo;
    private Long merchantId;
    private String status;
    private String reason;
    private String evidenceNote;
    private String resolution;
    private String openedBy;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
