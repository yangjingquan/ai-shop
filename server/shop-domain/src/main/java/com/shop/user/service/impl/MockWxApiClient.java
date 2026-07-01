package com.shop.user.service.impl;

import com.shop.user.service.WxApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev 环境使用：把 code 直接当 openid 前缀返回。便于本地联调，无需真实 appid/secret。
 * 同一个 code 多次调用映射到同一个 openid，方便测试新用户/老用户分支。
 */
@Slf4j
@Component
@Profile("mock-wx")
public class MockWxApiClient implements WxApiClient {

    @Override
    public String code2Openid(String appid, String secret, String jsCode) {
        String openid = "dev_openid_" + appid + "_" + jsCode;
        log.info("[MockWxApiClient] appid={} code={} -> openid={}", appid, jsCode, openid);
        return openid;
    }
}
