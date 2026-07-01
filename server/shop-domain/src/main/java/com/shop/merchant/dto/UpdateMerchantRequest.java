package com.shop.merchant.dto;

import jakarta.validation.constraints.Pattern;
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

    @Size(max = 32)
    @Pattern(regexp = "^$|^[0-9]{6,32}$", message = "微信支付商户号需为 6-32 位数字")
    private String wxMchId;
}
