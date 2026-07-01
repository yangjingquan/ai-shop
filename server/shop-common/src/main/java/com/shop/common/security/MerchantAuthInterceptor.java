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
public class MerchantAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        token = token.substring(7);
        if (!jwtUtil.isValid(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(token);
        String userType = claims.get("userType", String.class);
        if (!UserType.MERCHANT.name().equals(userType)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        CurrentUser user = CurrentUser.builder()
                .userId(claims.get("userId", Long.class))
                .merchantId(claims.get("merchantId", Long.class))
                .userType(UserType.MERCHANT)
                .build();
        CurrentUserHolder.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
