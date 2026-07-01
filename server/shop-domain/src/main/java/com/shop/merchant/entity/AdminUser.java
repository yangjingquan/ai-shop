package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUser extends BaseEntity {
    private String username;
    private String passwordHash;
    private String role;
}
