package com.shop.marketing.dto;

import lombok.Data;

@Data
public class MarketingFeatureVO {
    private String code;
    private String name;
    private String description;
    private Integer enabled;
    private Boolean implemented;
    private String frontendPath;
    private Integer sort;
}
