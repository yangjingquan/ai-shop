package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_logistics_trace")
public class OrderLogisticsTrace extends BaseEntity {

    private String orderNo;

    private String shipperCode;

    private String logisticCode;

    private String state;

    private LocalDateTime acceptTime;

    private String acceptStation;

    private String traceHash;
}
