package com.shop.common.security;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class WxAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenVersionService tokenVersionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("wx-token");
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!jwtUtil.isValid(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(token);
        String userType = claims.get("userType", String.class);
        if (!UserType.WX.name().equals(userType)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long userId = claims.get("userId", Long.class);
        Number tokenVersion = claims.get("tokenVersion", Number.class);
        if (tokenVersion == null || !tokenVersionService.isCurrent(UserType.WX, userId, tokenVersion.intValue())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        CurrentUser user = CurrentUser.builder()
                .userId(userId)
                .merchantId(claims.get("merchantId", Long.class))
                .userType(UserType.WX)
                .build();
        CurrentUserHolder.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
