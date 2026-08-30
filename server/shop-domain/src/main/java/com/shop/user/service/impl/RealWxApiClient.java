package com.shop.user.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.user.service.WxApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Slf4j
@Component
public class RealWxApiClient implements WxApiClient {

    @Override
    public String code2Openid(String appid, String secret, String jsCode) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            encode(appid), encode(secret), encode(jsCode));
        String resp = HttpUtil.get(url, 5000);
        JSONObject json = JSONUtil.parseObj(resp);
        if (!json.containsKey("openid")) {
            log.warn("wx jscode2session failed, appid={}, errcode={}, errmsg={}",
                    maskAppid(appid), json.getInt("errcode"), json.getStr("errmsg"));
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        }
        return json.getStr("openid");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String maskAppid(String value) {
        if (value == null || value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
