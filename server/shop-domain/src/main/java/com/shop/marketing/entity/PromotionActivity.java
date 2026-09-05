package com.shop.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_activity")
public class PromotionActivity extends BaseEntity {
    private Long merchantId;
    private String name;
    private String activityType;
    private Integer priority;
    private Integer status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer scopeType;
    private Integer stackNewUserCoupon;
    private Integer stackRepurchaseCoupon;
    private Integer showRecommendations;
    private BigDecimal budgetAmount;
    private Integer maxOrderCount;
    private BigDecimal reservedBudget;
    private Integer reservedOrderCount;
    private BigDecimal paidBudget;
    private Integer paidOrderCount;
}
