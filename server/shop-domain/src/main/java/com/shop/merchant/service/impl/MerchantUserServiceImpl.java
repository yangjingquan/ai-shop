package com.shop.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.merchant.dto.LoginResponse;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantUser;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.mapper.MerchantUserMapper;
import com.shop.merchant.security.PasswordPolicy;
import com.shop.merchant.service.MerchantUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantUserServiceImpl implements MerchantUserService {

    private final MerchantUserMapper merchantUserMapper;
    private final MerchantMapper merchantMapper;
    private final JwtUtil jwtUtil;
    @Value("${spring.profiles.active:dev}")
    private String activeProfiles;
    @Value("${SHOP_BOOTSTRAP_MERCHANT_PASSWORD:}")
    private String bootstrapPassword;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(String username, String password) {
        if (isProduction() && "merchant01".equalsIgnoreCase(username) && "merchant123".equals(password)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        MerchantUser user = merchantUserMapper.selectOne(
            new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getUsername, username)
        );
        boolean bootstrapLogin = isProduction() && "merchant01".equalsIgnoreCase(username)
                && bootstrapPassword != null && !bootstrapPassword.isBlank()
                && bootstrapPassword.equals(password) && !"merchant123".equals(password);
        if (user == null || (!bootstrapLogin && !ENCODER.matches(password, user.getPasswordHash()))) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        Merchant merchant = merchantMapper.selectById(user.getMerchantId());
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() == 0) {
            throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
        }
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String token = jwtUtil.generateToken(
            UserType.MERCHANT,
            Map.of("userId", user.getId(), "merchantId", user.getMerchantId(), "tokenVersion", tokenVersion)
        );
        return new LoginResponse(token, user.getRole(), user.getMerchantId());
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        PasswordPolicy.validate(newPassword);
        MerchantUser user = merchantUserMapper.selectById(userId);
        if (user == null || !matchesPassword(user, currentPassword)) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }
        user.setPasswordHash(ENCODER.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        merchantUserMapper.updateById(user);
    }

    private boolean matchesPassword(MerchantUser user, String password) {
        boolean bootstrapLogin = isProduction() && "merchant01".equalsIgnoreCase(user.getUsername())
                && bootstrapPassword != null && !bootstrapPassword.isBlank()
                && bootstrapPassword.equals(password) && !"merchant123".equals(password);
        return bootstrapLogin || ENCODER.matches(password, user.getPasswordHash());
    }

    private boolean isProduction() {
        return activeProfiles != null && java.util.Arrays.stream(activeProfiles.split(","))
                .map(String::trim).anyMatch("prod"::equalsIgnoreCase);
    }
}
