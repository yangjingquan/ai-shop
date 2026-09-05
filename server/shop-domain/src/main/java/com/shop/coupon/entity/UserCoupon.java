package com.shop.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {
    private Long userId;
    private Long merchantId;
    private Long templateId;
    private String issueScene;
    private String sourceOrderNo;
    private String templateNameSnapshot;
    private Integer type;
    private BigDecimal amountSnapshot;
    private BigDecimal thresholdSnapshot;
    private Integer scopeTypeSnapshot;
    private String scopeIdsSnapshot;
    private Integer excludeActivityGoodsSnapshot;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer status;
    private LocalDateTime receivedAt;
    private LocalDateTime usedAt;
    private String usedOrderNo;
    private String invalidReason;
}
