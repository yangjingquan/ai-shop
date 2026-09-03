package com.shop.common.security;

public interface MerchantPermissionChecker {
    boolean hasPermission(Long userId, Long merchantId, String permission);
}
