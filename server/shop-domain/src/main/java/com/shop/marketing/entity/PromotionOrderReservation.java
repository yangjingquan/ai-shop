package com.shop.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_order_reservation")
public class PromotionOrderReservation extends BaseEntity {
    private Long activityId;
    private String orderNo;
    private BigDecimal qualifiedAmount;
    private BigDecimal discountAmount;
    private String snapshotJson;
    private Integer status;
}
