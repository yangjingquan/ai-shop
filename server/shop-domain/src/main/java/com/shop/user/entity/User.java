package com.shop.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`user`")
public class User extends BaseEntity {
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private String phone;
    private LocalDateTime lastLoginAt;
}
