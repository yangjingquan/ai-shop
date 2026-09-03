package com.shop.merchant.dto;

import lombok.Data;

@Data
public class MerchantPermissionVO {
    private Long id;
    private String code;
    private String name;
    private String module;
    private String type;
    private Long parentId;
    private Integer sort;
}
