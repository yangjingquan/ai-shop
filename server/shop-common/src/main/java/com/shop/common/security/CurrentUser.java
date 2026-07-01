package com.shop.common.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUser {
    private Long userId;
    private Long merchantId;  // 仅 MERCHANT 类型有值
    private UserType userType;
}
