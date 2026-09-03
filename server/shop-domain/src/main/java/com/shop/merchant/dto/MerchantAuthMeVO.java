package com.shop.merchant.dto;

import lombok.Data;

import java.util.List;

@Data
public class MerchantAuthMeVO {
    private Long userId;
    private String username;
    private Long merchantId;
    private String merchantName;
    private List<MerchantRoleVO> roles;
    private List<String> permissions;
}
