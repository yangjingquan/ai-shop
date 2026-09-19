package com.shop.freight.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("freight_template")
public class FreightTemplate extends BaseEntity {
    private Long merchantId; private String name; private Integer enabled; private Long version;
}
