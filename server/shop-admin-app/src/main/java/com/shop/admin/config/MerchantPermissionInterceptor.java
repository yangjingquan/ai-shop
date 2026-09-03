package com.shop.admin.config;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.MerchantPermissionChecker;
import com.shop.common.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class MerchantPermissionInterceptor implements HandlerInterceptor {

    private final MerchantPermissionChecker permissionChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (required == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || !permissionChecker.hasPermission(user.getUserId(), user.getMerchantId(), required.value())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
