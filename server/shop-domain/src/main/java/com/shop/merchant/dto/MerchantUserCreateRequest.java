package com.shop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MerchantUserCreateRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名 4-20 位字母数字下划线")
    private String username;

    @NotBlank
    @Size(max = 1024, message = "密码参数过长")
    private String password;

    @NotEmpty
    private List<Long> roleIds;
}
