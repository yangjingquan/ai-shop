package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private Integer status;

    /** 0 普通订单 / 1 团购订单 */
    private Integer orderType;

    private Long groupBuyGroupId;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private Integer payMethod;

    private LocalDateTime payTime;

    private String payTransactionId;

    /** JSON string，service 层序列化 AddressSnapshot */
    private String addressSnapshot;

    private String shipNo;

    private String shipCompany;

    private LocalDateTime shipTime;

    private LocalDateTime shipReminderAt;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;
}
