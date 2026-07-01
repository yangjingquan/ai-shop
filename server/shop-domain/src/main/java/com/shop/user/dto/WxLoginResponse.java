package com.shop.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WxLoginResponse {
    private String token;
    private String openid;
    private Boolean isNewUser;
    private Boolean hasPhone;
}
