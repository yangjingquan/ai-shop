package com.shop.pricing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pricing_rule_version")
public class PricingRuleVersion extends BaseEntity {
    private Long version;
    private String matrixJson;
    private String zeroPayWhitelistJson;
    private BigDecimal maxDiscountRate;
    private Integer status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
}
