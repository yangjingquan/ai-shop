package com.shop.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_session")
public class SeckillSession extends BaseEntity {
    private Long activityId;
    private Long merchantId;
    private String name;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer sort;
}
