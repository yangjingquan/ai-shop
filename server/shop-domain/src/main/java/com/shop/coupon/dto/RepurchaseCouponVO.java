package com.shop.coupon.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RepurchaseCouponVO extends CouponVO {
    private String issueStatus;
    private String sourceOrderNo;
}
