package com.shop.presale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("presale_sku")
public class PresaleSku extends BaseEntity {
    private Long activityId;
    private Long merchantId;
    private Long productId;
    private Long skuId;
    private BigDecimal finalPrice;
    private BigDecimal depositAmount;
    private BigDecimal depositDeductionAmount;
    private BigDecimal balanceAmount;
    private Integer userLimit;
    private Integer depositCount;
    private Integer balanceSoldCount;
}
