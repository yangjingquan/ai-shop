package com.shop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMerchantRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名 4-20 位字母数字下划线")
    private String username;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;

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
