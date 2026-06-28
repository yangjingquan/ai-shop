package com.shop.user.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.aop.RateLimit;
import com.shop.user.dto.WxLoginRequest;
import com.shop.user.dto.WxLoginResponse;
import com.shop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/auth")
@RequiredArgsConstructor
public class WxAuthController {

    private final UserService userService;

    @RateLimit(key = "login", limit = 5, windowSec = 60)
    @PostMapping("/login")
    public ApiResult<WxLoginResponse> login(@RequestBody @Valid WxLoginRequest req) {
        return ApiResult.success(userService.wxLogin(req.getCode()));
    }
}
