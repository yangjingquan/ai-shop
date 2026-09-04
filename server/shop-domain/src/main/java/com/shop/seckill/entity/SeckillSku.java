package com.shop.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_sku")
public class SeckillSku extends BaseEntity {
    private Long sessionId;
    private Long merchantId;
    private Long productId;
    private Long skuId;
    private BigDecimal activityPrice;
    private Integer activityStock;
    private Integer soldCount;
    private Integer userLimit;
}
