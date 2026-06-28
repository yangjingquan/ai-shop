package com.shop.merchant.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UpdateMerchantSelfRequest {
    @Length(max = 255)
    private String logo;

    @Length(max = 500)
    private String description;

    @Length(max = 255)
    private String address;

    @Length(max = 32)
    private String contactName;

    @Length(max = 20)
    private String contactPhone;
}
