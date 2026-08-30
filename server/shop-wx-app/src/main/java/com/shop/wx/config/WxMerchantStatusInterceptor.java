package com.shop.wx.config;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUserHolder;
import com.shop.merchant.mapper.MerchantMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 是有时效的，不能因为商家被冻结后旧 Token 尚未过期就继续允许 C 端写入。
 */
@Component
@RequiredArgsConstructor
public class WxMerchantStatusInterceptor implements HandlerInterceptor {

    private final MerchantMapper merchantMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var current = CurrentUserHolder.get();
        if (current == null || current.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        var merchant = merchantMapper.selectById(current.getMerchantId());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
        }
        return true;
    }
}
