package com.shop.user.service.impl;

import com.shop.user.service.WxPhoneApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Prod 环境骨架：调用微信 phonenumber.getPhoneNumber 解密手机号。
 * M2 仅留接口签名，避免 prod profile 启动时缺少实现报错。
 */
@Slf4j
@Component
@Profile("!dev")
public class RealWxPhoneApiClient implements WxPhoneApiClient {

    @Override
    public String code2Phone(String code) {
        // TODO: 调微信 phonenumber.getPhoneNumber
        // 1. 从 Redis 取 access_token，无则调 cgi-bin/token 获取并缓存（TTL = expires_in - 60s）
        // 2. POST https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=...
        //    body: { "code": code }
        // 3. 解析返回 phone_info.phoneNumber
        throw new UnsupportedOperationException("RealWxPhoneApiClient not implemented yet");
    }
}
