package com.shop.user.service;

/**
 * 微信手机号解密接口。
 * 通过微信官方接口换取用户绑定手机号。
 */
public interface WxPhoneApiClient {
    String code2Phone(String appid, String secret, String code);
}
