package com.shop.merchant.dto;

import lombok.Data;

import java.util.List;

@Data
public class MerchantRoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer builtin;
    private Integer status;
    private Integer userCount;
    private List<String> permissionCodes;
}
