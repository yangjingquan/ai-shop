package com.shop.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_activity")
public class SeckillActivity extends BaseEntity {
    private Long merchantId;
    private String name;
    private String description;
    private LocalDateTime preheatAt;
    private Integer status;
    private Long createdBy;
}
