package com.shop.home.dto;

import lombok.Data;

@Data
public class StorefrontTemplateVO {
    private String code;
    private String name;
    private String description;
    private String pageType;
    private StorefrontDocument document;
}
