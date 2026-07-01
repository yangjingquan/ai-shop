package com.shop.merchant.service;

import com.shop.merchant.dto.LoginResponse;

public interface MerchantUserService {
    LoginResponse login(String username, String password);
}
