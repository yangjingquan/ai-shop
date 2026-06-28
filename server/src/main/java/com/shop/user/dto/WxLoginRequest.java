package com.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WxLoginRequest {
    @NotBlank(message = "code不能为空")
    private String code;

    @NotBlank(message = "商户代码不能为空")
    @Pattern(regexp = "^M[A-Z0-9]{6,31}$", message = "商户代码格式错误")
    private String merchantCode;
}
