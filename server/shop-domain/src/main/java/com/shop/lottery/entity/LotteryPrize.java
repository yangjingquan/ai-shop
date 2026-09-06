package com.shop.lottery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lottery_prize")
public class LotteryPrize extends BaseEntity {
    private Long activityId;
    private Long merchantId;
    private String name;
    private String prizeType;
    private String image;
    private Integer pointsAmount;
    private Long couponTemplateId;
    private Long productId;
    private Long skuId;
    private Integer totalStock;
    private Integer remainingStock;
    private BigDecimal probability;
    private Integer validityDays;
    private Integer sort;
    private Integer status;
}
