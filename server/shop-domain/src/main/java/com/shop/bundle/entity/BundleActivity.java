package com.shop.bundle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bundle_activity")
public class BundleActivity extends BaseEntity {
    private Long merchantId;
    private String name;
    private Long mainProductId;
    private BigDecimal discountAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    private Long createdBy;
}
