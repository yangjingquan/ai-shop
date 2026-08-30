package com.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WxLoginRequest {
    @NotBlank(message = "code不能为空")
    private String code;

    @Pattern(regexp = "^M[A-Z0-9]{6,31}$", message = "商户代码格式错误")
    private String merchantCode;

    @Pattern(regexp = "^wx[A-Za-z0-9_-]{6,62}$", message = "小程序 AppID 格式错误")
    private String miniAppId;

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode == null ? null : merchantCode.trim();
    }

    public void setMiniAppId(String miniAppId) {
        this.miniAppId = miniAppId == null ? null : miniAppId.trim();
    }
}
