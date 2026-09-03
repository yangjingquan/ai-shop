package com.shop.merchant.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantUserVO {
    private Long id;
    private String username;
    private Integer status;
    private LocalDateTime createdAt;
    private List<MerchantRoleVO> roles;
}
