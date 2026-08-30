package com.shop.common.security;

/** 提供认证层所需的令牌版本校验，具体数据源由业务模块实现。 */
public interface TokenVersionService {

    boolean isCurrent(UserType userType, Long userId, int tokenVersion);
}
