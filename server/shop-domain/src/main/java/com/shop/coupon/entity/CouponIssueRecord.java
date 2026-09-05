package com.shop.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_issue_record")
public class CouponIssueRecord extends BaseEntity {
    private Long merchantId;
    private Long userId;
    private String sourceOrderNo;
    private Long templateId;
    private Long userCouponId;
    private String issueScene;
    private Integer status;
    private String idempotencyKey;
    private String skipReason;
    private Long refundId;
    private LocalDateTime revokedAt;
}
