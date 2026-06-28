package com.shop.user.service.impl;

import com.shop.user.service.WxPhoneApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev 环境：固定返回 13800000000，便于本地联调。
 */
@Slf4j
@Component
@Profile("dev")
public class MockWxPhoneApiClient implements WxPhoneApiClient {

    @Override
    public String code2Phone(String code) {
        int suffix = Math.abs((code == null ? "" : code).hashCode()) % 100_000_000;
        String phone = "139" + String.format("%08d", suffix);
        log.info("[MockWxPhoneApiClient] code={} -> phone={}", code, phone);
        return phone;
    }
}
