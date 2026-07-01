package com.shop.merchant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantSelfVO {
    private Long id;
    private String merchantCode;
    private String name;
    private String logo;
    private String description;
    private String address;
    private String contactName;
    private String contactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
