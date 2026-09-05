package com.shop.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_scope")
public class PromotionScope extends BaseEntity {
    private Long activityId;
    private Integer targetType;
    private Long targetId;
}
