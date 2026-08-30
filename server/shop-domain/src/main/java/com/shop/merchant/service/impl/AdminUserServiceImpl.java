package com.shop.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.merchant.dto.LoginResponse;
import com.shop.merchant.entity.AdminUser;
import com.shop.merchant.mapper.AdminUserMapper;
import com.shop.merchant.security.PasswordPolicy;
import com.shop.merchant.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;
    @Value("${spring.profiles.active:dev}")
    private String activeProfiles;
    @Value("${SHOP_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String bootstrapPassword;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(String username, String password) {
        if (isProduction() && "admin".equalsIgnoreCase(username) && "admin123".equals(password)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        AdminUser user = adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username)
        );
        boolean bootstrapLogin = isProduction() && "admin".equalsIgnoreCase(username)
                && bootstrapPassword != null && !bootstrapPassword.isBlank()
                && bootstrapPassword.equals(password) && !"admin123".equals(password);
        if (user == null || (!bootstrapLogin && !ENCODER.matches(password, user.getPasswordHash()))) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String token = jwtUtil.generateToken(UserType.ADMIN,
                Map.of("userId", user.getId(), "tokenVersion", tokenVersion));
        return new LoginResponse(token, user.getRole(), null);
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        PasswordPolicy.validate(newPassword);
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null || !matchesPassword(user, currentPassword)) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }
        user.setPasswordHash(ENCODER.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        adminUserMapper.updateById(user);
    }

    private boolean matchesPassword(AdminUser user, String password) {
        boolean bootstrapLogin = isProduction() && "admin".equalsIgnoreCase(user.getUsername())
                && bootstrapPassword != null && !bootstrapPassword.isBlank()
                && bootstrapPassword.equals(password) && !"admin123".equals(password);
        return bootstrapLogin || ENCODER.matches(password, user.getPasswordHash());
    }

    private boolean isProduction() {
        return activeProfiles != null && java.util.Arrays.stream(activeProfiles.split(","))
                .map(String::trim).anyMatch("prod"::equalsIgnoreCase);
    }
}
