package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_log")
public class PaymentLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String transactionId;

    private BigDecimal amount;

    /** JSON string */
    private String rawPayload;

    private LocalDateTime createdAt;
}
