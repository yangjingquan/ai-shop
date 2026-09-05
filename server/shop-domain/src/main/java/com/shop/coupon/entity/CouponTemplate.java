package com.shop.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_template")
public class CouponTemplate extends BaseEntity {
    private Long merchantId;
    private String name;
    private String image;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal thresholdAmount;
    private Integer totalStock;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer perUserLimit;
    private Integer validityDays;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer scopeType;
    private String scopeIdsJson;
    private Integer newUserOnly;
    private String issueScene;
    private Integer repurchaseTargetType;
    private String repurchaseTargetIdsJson;
    private BigDecimal repurchaseMinOrderAmount;
    private Integer repurchaseFirstPurchaseOnly;
    private Integer repurchasePriority;
    private Integer excludeActivityGoods;
    private Integer stackable;
    private Integer status;
}
