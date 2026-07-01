package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_user")
public class MerchantUser extends BaseEntity {
    private Long merchantId;
    private String username;
    private String passwordHash;
    private String role;
}
