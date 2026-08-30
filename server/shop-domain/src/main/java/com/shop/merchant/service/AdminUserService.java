package com.shop.merchant.service;

import com.shop.merchant.dto.LoginResponse;

public interface AdminUserService {
    LoginResponse login(String username, String password);

    void changePassword(Long userId, String currentPassword, String newPassword);
}
