package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_role")
public class MerchantRole extends BaseEntity {
    private Long merchantId;
    private String code;
    private String name;
    private String description;
    private Integer builtin;
    private Integer status;
    private Integer sort;
}
