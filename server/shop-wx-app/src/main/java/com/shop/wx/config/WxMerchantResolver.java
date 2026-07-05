package com.shop.wx.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.JwtUtil;
import com.shop.common.security.UserType;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WxMerchantResolver {

    private final JwtUtil jwtUtil;
    private final MerchantMapper merchantMapper;

    public Long currentMerchantId(HttpServletRequest request) {
        CurrentUser current = CurrentUserHolder.get();
        if (current != null && current.getMerchantId() != null) {
            return current.getMerchantId();
        }

        String token = request.getHeader("wx-token");
        if (StringUtils.hasText(token) && jwtUtil.isValid(token)) {
            Claims claims = jwtUtil.parseToken(token);
            String userType = claims.get("userType", String.class);
            if (UserType.WX.name().equals(userType)) {
                Long merchantId = claims.get("merchantId", Long.class);
                if (merchantId != null) {
                    return merchantId;
                }
            }
        }

        String merchantCode = request.getHeader("merchant-code");
        if (!StringUtils.hasText(merchantCode)) {
            merchantCode = request.getParameter("merchantCode");
        }
        if (!StringUtils.hasText(merchantCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "缺少商家编码");
        }

        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantCode, merchantCode.trim())
                        .eq(Merchant::getStatus, 1)
                        .last("limit 1"));
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return merchant.getId();
    }
}
