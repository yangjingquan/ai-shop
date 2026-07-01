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
import com.shop.merchant.service.MerchantUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantUserServiceImpl implements MerchantUserService {

    private final MerchantUserMapper merchantUserMapper;
    private final MerchantMapper merchantMapper;
    private final JwtUtil jwtUtil;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(String username, String password) {
        MerchantUser user = merchantUserMapper.selectOne(
            new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getUsername, username)
        );
        if (user == null || !ENCODER.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        Merchant merchant = merchantMapper.selectById(user.getMerchantId());
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() == 0) {
            throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
        }
        String token = jwtUtil.generateToken(
            UserType.MERCHANT,
            Map.of("userId", user.getId(), "merchantId", user.getMerchantId())
        );
        return new LoginResponse(token, user.getRole(), user.getMerchantId());
    }
}
