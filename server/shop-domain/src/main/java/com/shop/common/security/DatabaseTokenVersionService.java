package com.shop.common.security;

import com.shop.merchant.entity.AdminUser;
import com.shop.merchant.entity.MerchantUser;
import com.shop.merchant.mapper.AdminUserMapper;
import com.shop.merchant.mapper.MerchantUserMapper;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseTokenVersionService implements TokenVersionService {

    private final AdminUserMapper adminUserMapper;
    private final MerchantUserMapper merchantUserMapper;
    private final UserMapper userMapper;

    @Override
    public boolean isCurrent(UserType userType, Long userId, int tokenVersion) {
        if (userId == null) return false;
        Integer persistedVersion = switch (userType) {
            case ADMIN -> versionOf(adminUserMapper.selectById(userId));
            case MERCHANT -> versionOf(merchantUserMapper.selectById(userId));
            case WX -> versionOf(userMapper.selectById(userId));
        };
        return persistedVersion != null && persistedVersion == tokenVersion;
    }

    private Integer versionOf(AdminUser user) {
        return user == null ? null : user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private Integer versionOf(MerchantUser user) {
        return user == null ? null : user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private Integer versionOf(User user) {
        return user == null ? null : user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }
}
