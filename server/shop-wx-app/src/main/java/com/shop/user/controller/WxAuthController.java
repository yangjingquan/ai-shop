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
    private static final Pattern MINI_APP_ID_PATTERN = Pattern.compile("^wx[A-Za-z0-9_-]{6,62}$");

    private final UserService userService;

    @RateLimit(key = "login", limit = 5, windowSec = 60)
    @PostMapping("/login")
    public ApiResult<WxLoginResponse> login(@RequestBody(required = false) WxLoginRequest req,
                                            @RequestParam(required = false) String code,
                                            @RequestParam(required = false) String merchantCode,
                                            @RequestParam(required = false) String miniAppId) {
        String finalCode = firstText(req == null ? null : req.getCode(), code);
        String finalMerchantCode = firstText(req == null ? null : req.getMerchantCode(), merchantCode);
        String finalMiniAppId = firstText(req == null ? null : req.getMiniAppId(), miniAppId);
        if (finalCode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "code不能为空，请传JSON body或query/form参数");
        }
        if (finalMerchantCode == null && finalMiniAppId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户代码和小程序 AppID 至少传一个");
        }
        if (finalMerchantCode != null && !MERCHANT_CODE_PATTERN.matcher(finalMerchantCode).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户代码格式错误");
        }
        if (finalMiniAppId != null && !MINI_APP_ID_PATTERN.matcher(finalMiniAppId).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "小程序 AppID 格式错误");
        }
        return ApiResult.success(finalMerchantCode != null
                ? userService.wxLogin(finalCode, finalMerchantCode)
                : userService.wxLoginByAppId(finalCode, finalMiniAppId));
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
