package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_state_transition")
public class OrderStateTransition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Integer fromState;
    private Integer toState;
    private String event;
    private String reason;
    private String payloadJson;
    private LocalDateTime createdAt;
}
