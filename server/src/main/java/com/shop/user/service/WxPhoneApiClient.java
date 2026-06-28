package com.shop.user.service;

/**
 * 微信手机号解密接口。
 * dev profile 走 Mock，prod profile 走 Real（M2 仅留骨架，未实现）。
 */
public interface WxPhoneApiClient {
    String code2Phone(String code);
}
