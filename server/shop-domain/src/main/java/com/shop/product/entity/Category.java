package com.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    private Long parentId;

    private String name;

    private String icon;

    private Integer level;

    private Integer sort;

    private Integer status;
}
