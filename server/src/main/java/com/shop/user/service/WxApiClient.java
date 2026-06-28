package com.shop.user.service;

public interface WxApiClient {
    /**
     * 调微信 jscode2session，返回 openid。失败抛 BusinessException。
     */
    String code2Openid(String jsCode);
}
