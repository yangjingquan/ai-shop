package com.shop.merchant.controller;

import com.shop.common.aop.RateLimit;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.merchant.dto.ChangePasswordRequest;
import com.shop.merchant.dto.LoginRequest;
import com.shop.merchant.dto.LoginResponse;
import com.shop.merchant.security.PasswordCipher;
import com.shop.merchant.service.MerchantUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantUserService merchantUserService;
    private final PasswordCipher passwordCipher;

    @GetMapping("/public-key")
    public ApiResult<String> publicKey() {
        return ApiResult.success(passwordCipher.getPublicKey());
    }

    @RateLimit(key = "login", limit = 5, windowSec = 60)
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return ApiResult.success(merchantUserService.login(req.getUsername(), passwordCipher.decrypt(req.getPassword())));
    }

    @PutMapping("/password")
    public ApiResult<Void> changePassword(@RequestBody @Valid ChangePasswordRequest req) {
        merchantUserService.changePassword(CurrentUserHolder.get().getUserId(),
                passwordCipher.decrypt(req.getCurrentPassword()), passwordCipher.decrypt(req.getNewPassword()));
        return ApiResult.success(null);
    }
}
