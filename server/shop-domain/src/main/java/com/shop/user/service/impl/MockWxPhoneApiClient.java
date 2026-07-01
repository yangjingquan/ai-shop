package com.shop.user.service.impl;

import com.shop.user.service.WxPhoneApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock 微信手机号接口：根据 code 生成测试手机号，便于本地联调。
 */
@Slf4j
@Component
@Profile("mock-wx")
public class MockWxPhoneApiClient implements WxPhoneApiClient {

    @Override
    public String code2Phone(String appid, String secret, String code) {
        int suffix = Math.abs((code == null ? "" : code).hashCode()) % 100_000_000;
        String phone = "139" + String.format("%08d", suffix);
        log.info("[MockWxPhoneApiClient] code={} -> phone={}", code, phone);
        return phone;
    }
}
