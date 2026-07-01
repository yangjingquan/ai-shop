package com.shop.user.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仅 dev/test profile 暴露：根据 userId 直接发 wx 用户 token,
 * 方便 curl/单测/小程序调试，不依赖 wx.login。
 */
@RestController
@RequestMapping("/api/wx/auth")
@RequiredArgsConstructor
@Profile("!prod")
public class WxDevLoginController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @PostMapping("/dev-login")
    public ApiResult<Map<String, Object>> devLogin(@RequestBody Map<String, Long> body) {
        Long userId = body == null ? null : body.get("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        String token = jwtUtil.generateToken(UserType.WX, claims);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        return ApiResult.success(data);
    }
}
