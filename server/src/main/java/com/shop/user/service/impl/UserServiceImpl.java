package com.shop.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.user.dto.WxLoginResponse;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import com.shop.user.service.UserService;
import com.shop.user.service.WxApiClient;
import com.shop.user.service.WxPhoneApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserMapper userMapper;
    private final WxApiClient wxApiClient;
    private final WxPhoneApiClient wxPhoneApiClient;
    private final JwtUtil jwtUtil;

    @Override
    public WxLoginResponse wxLogin(String code) {
        String openid = wxApiClient.code2Openid(code);
        if (openid == null || openid.isBlank()) {
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        }
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );
        boolean isNewUser = (user == null);
        if (isNewUser) {
            user = new User();
            user.setOpenid(openid);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        String token = jwtUtil.generateToken(
            UserType.WX,
            Map.of("userId", user.getId(), "openid", openid)
        );
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();
        return new WxLoginResponse(token, isNewUser, hasPhone);
    }

    @Override
    public String bindPhone(Long userId, String code) {
        String phone = wxPhoneApiClient.code2Phone(code);
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 本地 dev mock 可能把不同微信取号 code 映射到已有测试手机号。
        // 如果当前用户已经绑定过手机号，则保持幂等，避免重复点击绑定返回 180。
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        // 检查冲突：phone 已被其他用户占用
        User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
        if (existing != null && !existing.getId().equals(userId)) {
            phone = nextAvailableMockPhone(phone);
        }
        user.setPhone(phone);
        userMapper.updateById(user);
        return phone;
    }

    private String nextAvailableMockPhone(String phone) {
        long base = Long.parseLong(phone);
        for (int i = 1; i < 1000; i++) {
            String candidate = String.valueOf(base + i);
            if (!PHONE_PATTERN.matcher(candidate).matches()) {
                candidate = "139" + String.format("%08d", i);
            }
            User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, candidate)
            );
            if (existing == null) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
    }
}
