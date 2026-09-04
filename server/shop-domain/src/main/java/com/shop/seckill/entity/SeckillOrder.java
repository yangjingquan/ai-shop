package com.shop.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_order")
public class SeckillOrder extends BaseEntity {
    private String orderNo;
    private Long merchantId;
    private Long activityId;
    private Long sessionId;
    private Long seckillSkuId;
    private Long productId;
    private Long skuId;
    private Long userId;
    private Integer quantity;
    private BigDecimal activityPrice;
    private Integer status;
    private Integer stockReleased;
}
