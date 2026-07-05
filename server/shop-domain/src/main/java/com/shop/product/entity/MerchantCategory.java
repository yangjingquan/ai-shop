package com.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_category")
public class MerchantCategory extends BaseEntity {

    private Long merchantId;

    private Long sourceCategoryId;

    private Long parentId;

    private String name;

    private String icon;

    private Integer level;

    private Integer sort;

    private Integer status;
}
