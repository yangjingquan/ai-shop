package com.shop.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_tag")
public class CustomerTag extends BaseEntity {
    private Long merchantId;
    private String code;
    private String name;
    private String tagType;
    private String color;
    private Integer status;
}
