package com.shop.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_metric_snapshot")
public class CustomerMetricSnapshot extends BaseEntity {
    private Long merchantId;
    private Long userId;
    private Integer paidOrderCount;
    private BigDecimal totalPaidAmount;
    private LocalDateTime firstPaidAt;
    private LocalDateTime lastPaidAt;
    private Integer memberLevel;
    private Integer pointsBalance;
    private Integer unusedCouponCount;
    private Integer expiringCouponCount;
    private Integer favoriteCount;
    private Integer historyCount;
    private LocalDateTime lastViewedAt;
    private LocalDateTime registeredAt;
    private LocalDateTime memberLevelUpdatedAt;
    private LocalDateTime calculatedAt;
}
