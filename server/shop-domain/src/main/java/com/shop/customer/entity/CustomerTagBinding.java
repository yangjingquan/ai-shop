package com.shop.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_tag_binding")
public class CustomerTagBinding extends BaseEntity {
    private Long merchantId;
    private Long userId;
    private Long tagId;
    private String source;
    private Integer status;
}
