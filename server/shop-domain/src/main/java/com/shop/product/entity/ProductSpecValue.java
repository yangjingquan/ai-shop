package com.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_spec_value")
public class ProductSpecValue extends BaseEntity {

    private Long specId;

    private String value;

    private Integer sort;
}
