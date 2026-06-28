package com.shop.user.service;

import com.shop.user.dto.WxLoginResponse;

public interface UserService {
    WxLoginResponse wxLogin(String code, String merchantCode);

    String bindPhone(Long userId, String code);
}
