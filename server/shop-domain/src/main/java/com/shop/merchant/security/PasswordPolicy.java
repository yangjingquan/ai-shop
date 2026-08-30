package com.shop.merchant.security;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "密码长度需为 8-32 位");
        }
    }
}
