package com.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserAddressRequest {
    @NotBlank
    @Length(max = 50)
    private String receiver;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Length(max = 100)
    private String region;

    @NotBlank
    @Length(max = 255)
    private String detail;

    private Boolean isDefault;
}
