package com.shop.user.service;

/**
 * 微信手机号解密接口。
 * mock-wx profile 走 Mock，其他 profile 走 Real。
 */
public interface WxPhoneApiClient {
    String code2Phone(String appid, String secret, String code);
}
