package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("merchant_user_role")
public class MerchantUserRole {
    private Long userId;
    private Long roleId;
}
