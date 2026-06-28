package com.shop.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.user.dto.WxLoginResponse;
import com.shop.user.dto.WxUserProfileVO;
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
    private final MerchantMapper merchantMapper;
    private final JwtUtil jwtUtil;

    @Override
    public WxLoginResponse wxLogin(String code, String merchantCode) {
        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getMerchantCode, merchantCode)
                .last("LIMIT 1")
        );
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (merchant.getWxAppId() == null || merchant.getWxAppId().isBlank()
            || merchant.getWxSecret() == null || merchant.getWxSecret().isBlank()) {
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        }

        String openid = wxApiClient.code2Openid(merchant.getWxAppId(), merchant.getWxSecret(), code);
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
            Map.of(
                "userId", user.getId(),
                "openid", openid,
                "merchantId", merchant.getId(),
                "merchantCode", merchant.getMerchantCode(),
                "appid", merchant.getWxAppId()
            )
        );
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();
        return new WxLoginResponse(token, openid, isNewUser, hasPhone);
    }

    @Override
    public String bindPhone(Long userId, Long merchantId, String code) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getWxAppId() == null || merchant.getWxAppId().isBlank()
                || merchant.getWxSecret() == null || merchant.getWxSecret().isBlank()) {
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        String phone = wxPhoneApiClient.code2Phone(merchant.getWxAppId(), merchant.getWxSecret(), code);
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
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        user.setPhone(phone);
        userMapper.updateById(user);
        return phone;
    }

    @Override
    public WxUserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        WxUserProfileVO vo = new WxUserProfileVO();
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        return vo;
    }

    @Override
    public void updateProfile(Long userId, String nickname, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (nickname != null && !nickname.trim().isBlank()) {
            user.setNickname(nickname.trim());
        }
        if (avatar != null && !avatar.trim().isBlank() && !isTemporaryAvatarUrl(avatar.trim())) {
            user.setAvatar(avatar.trim());
        }
        userMapper.updateById(user);
    }

    private boolean isTemporaryAvatarUrl(String avatar) {
        return avatar.startsWith("wxfile://")
                || avatar.startsWith("blob:")
                || avatar.startsWith("data:");
    }
}
