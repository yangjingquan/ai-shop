package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("merchant_role_permission")
public class MerchantRolePermission {
    private Long roleId;
    private Long permissionId;
}
