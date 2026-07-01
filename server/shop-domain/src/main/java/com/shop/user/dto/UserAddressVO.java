package com.shop.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAddressVO {
    private Long id;
    private String receiver;
    private String phone;
    private String region;
    private String detail;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
