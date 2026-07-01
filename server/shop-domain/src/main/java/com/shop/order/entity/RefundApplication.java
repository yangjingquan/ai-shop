package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("refund_application")
public class RefundApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private String reason;

    private Integer status;

    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
