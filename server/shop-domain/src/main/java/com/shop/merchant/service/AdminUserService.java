package com.shop.merchant.service;

import com.shop.merchant.dto.LoginResponse;

public interface AdminUserService {
    LoginResponse login(String username, String password);
}
