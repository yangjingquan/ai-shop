package com.shop.user.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.user.service.WxApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!dev")
public class RealWxApiClient implements WxApiClient {

    @Value("${shop.wechat.appid}")
    private String appid;

    @Value("${shop.wechat.secret}")
    private String secret;

    @Override
    public String code2Openid(String jsCode) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid, secret, jsCode);
        String resp = HttpUtil.get(url, 5000);
        log.debug("wx jscode2session resp: {}", resp);
        JSONObject json = JSONUtil.parseObj(resp);
        if (!json.containsKey("openid")) {
            log.warn("wx jscode2session failed: {}", resp);
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        }
        return json.getStr("openid");
    }
}
