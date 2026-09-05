package com.shop.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_threshold")
public class PromotionThreshold extends BaseEntity {
    private Long activityId;
    private BigDecimal thresholdAmount;
    private BigDecimal reductionAmount;
    private BigDecimal discountRate;
    private BigDecimal discountCap;
    private Integer sort;
}
