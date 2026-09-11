package com.shop.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private Long merchantId;
    private String merchantName;
    private String nickname;
    private String avatar;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
