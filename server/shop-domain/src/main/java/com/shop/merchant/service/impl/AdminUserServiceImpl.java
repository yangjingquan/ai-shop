package com.shop.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.merchant.dto.LoginResponse;
import com.shop.merchant.entity.AdminUser;
import com.shop.merchant.mapper.AdminUserMapper;
import com.shop.merchant.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(String username, String password) {
        AdminUser user = adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username)
        );
        if (user == null || !ENCODER.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        String token = jwtUtil.generateToken(UserType.ADMIN, Map.of("userId", user.getId()));
        return new LoginResponse(token, user.getRole(), null);
    }
}
