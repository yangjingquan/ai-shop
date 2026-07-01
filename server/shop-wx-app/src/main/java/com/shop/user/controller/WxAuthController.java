package com.shop.user.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.aop.RateLimit;
import com.shop.user.dto.WxLoginRequest;
import com.shop.user.dto.WxLoginResponse;
import com.shop.user.service.UserService;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/auth")
@RequiredArgsConstructor
public class WxAuthController {

    private static final Pattern MERCHANT_CODE_PATTERN = Pattern.compile("^M[A-Z0-9]{6,31}$");

    private final UserService userService;

    @RateLimit(key = "login", limit = 5, windowSec = 60)
    @PostMapping("/login")
    public ApiResult<WxLoginResponse> login(@RequestBody(required = false) WxLoginRequest req,
                                            @RequestParam(required = false) String code,
                                            @RequestParam(required = false) String merchantCode) {
        String finalCode = firstText(req == null ? null : req.getCode(), code);
        String finalMerchantCode = firstText(req == null ? null : req.getMerchantCode(), merchantCode);
        if (finalCode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "code不能为空，请传JSON body或query/form参数");
        }
        if (finalMerchantCode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户代码不能为空，请传JSON body或query/form参数");
        }
        if (!MERCHANT_CODE_PATTERN.matcher(finalMerchantCode).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户代码格式错误");
        }
        return ApiResult.success(userService.wxLogin(finalCode, finalMerchantCode));
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
