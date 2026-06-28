package com.shop.merchant.controller;

import com.shop.common.aop.RateLimit;
import com.shop.common.response.ApiResult;
import com.shop.merchant.dto.LoginRequest;
import com.shop.merchant.dto.LoginResponse;
import com.shop.merchant.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserService adminUserService;

    @RateLimit(key = "login", limit = 5, windowSec = 60)
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return ApiResult.success(adminUserService.login(req.getUsername(), req.getPassword()));
    }
}
