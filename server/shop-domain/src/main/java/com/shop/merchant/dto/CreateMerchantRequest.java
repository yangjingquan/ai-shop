package com.shop.merchant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Size(max = 32)
    @Pattern(regexp = "^$|^[0-9]{6,32}$", message = "微信支付商户号需为 6-32 位数字")
    private String wxMchId;

    @Size(max = 128)
    private String wxPayApiV3Key;

    @Size(max = 128)
    private String wxPayMchSerialNo;

    @Size(max = 8192)
    private String wxPayPrivateKey;

    @Size(max = 255)
    private String wxPayNotifyUrl;

    @Min(0)
    @Max(1)
    private Integer wxPayEnabled;
}
