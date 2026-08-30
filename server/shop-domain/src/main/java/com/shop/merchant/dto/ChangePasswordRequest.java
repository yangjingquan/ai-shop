package com.shop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码不能为空")
    @Size(max = 1024, message = "密码参数过长")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 1024, message = "密码参数过长")
    private String newPassword;
}
