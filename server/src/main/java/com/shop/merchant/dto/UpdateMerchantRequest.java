package com.shop.merchant.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMerchantRequest {

    @Size(max = 50)
    private String name;

    private String contactName;
    private String contactPhone;
    private String description;
    private String address;
    private String logo;

    @Size(max = 64)
    private String wxAppId;

    @Size(max = 128)
    private String wxSecret;
}
