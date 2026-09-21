package com.shop.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_segment")
public class CustomerSegment extends BaseEntity {
    private Long merchantId;
    private String name;
    private String description;
    private String segmentType;
    private String conditionJson;
    private Integer memberCount;
    private LocalDateTime calculatedAt;
    private Integer status;
}
