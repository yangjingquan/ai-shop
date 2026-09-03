package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_permission")
public class MerchantPermission extends BaseEntity {
    private String code;
    private String name;
    private String module;
    private String type;
    private Long parentId;
    private Integer sort;
}
