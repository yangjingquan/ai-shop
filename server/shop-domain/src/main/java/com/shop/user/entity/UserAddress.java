package com.shop.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_address")
public class UserAddress extends BaseEntity {
    private Long userId;
    private String receiver;
    private String phone;
    private String region;
    private String detail;
    private Integer isDefault;
}
