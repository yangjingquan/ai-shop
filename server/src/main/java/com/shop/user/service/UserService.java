package com.shop.user.service;

import com.shop.user.dto.WxLoginResponse;
import com.shop.user.dto.WxUserProfileVO;

public interface UserService {
    WxLoginResponse wxLogin(String code, String merchantCode);

    String bindPhone(Long userId, Long merchantId, String code);

    WxUserProfileVO getProfile(Long userId);

    void updateProfile(Long userId, String nickname, String avatar);
}
