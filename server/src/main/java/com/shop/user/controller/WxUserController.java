package com.shop.user.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.user.dto.BindPhoneRequest;
import com.shop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/user")
@RequiredArgsConstructor
public class WxUserController {

    private final UserService userService;

    @PostMapping("/bind-phone")
    public ApiResult<String> bindPhone(@RequestBody @Valid BindPhoneRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        String phone = userService.bindPhone(userId, req.getCode());
        return ApiResult.success(phone);
    }
}
