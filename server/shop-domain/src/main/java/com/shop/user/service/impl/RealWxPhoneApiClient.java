package com.shop.user.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.user.service.WxPhoneApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 真实微信手机号接口：使用 getPhoneNumber code 换取用户绑定手机号。
 */
@Slf4j
@Component
@Profile("!mock-wx")
@RequiredArgsConstructor
public class RealWxPhoneApiClient implements WxPhoneApiClient {

    private static final String ACCESS_TOKEN_KEY_PREFIX = "wx:access_token:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String code2Phone(String appid, String secret, String code) {
        if (appid == null || appid.isBlank() || secret == null || secret.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        String accessToken = getAccessToken(appid, secret);
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        String body = JSONUtil.createObj().set("code", code).toString();
        String resp = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(5000)
                .execute()
                .body();
        JSONObject json = JSONUtil.parseObj(resp);
        Integer errcode = json.getInt("errcode", -1);
        if (errcode == null || errcode != 0) {
            log.warn("wx getuserphonenumber failed, appid={}, errcode={}, errmsg={}", maskAppid(appid), errcode, json.getStr("errmsg"));
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        JSONObject phoneInfo = json.getJSONObject("phone_info");
        String phone = phoneInfo == null ? null : phoneInfo.getStr("phoneNumber");
        if (phone == null || phone.isBlank()) {
            log.warn("wx getuserphonenumber missing phoneNumber, appid={}, resp={}", maskAppid(appid), resp);
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        return phone;
    }

    private String getAccessToken(String appid, String secret) {
        String key = ACCESS_TOKEN_KEY_PREFIX + appid;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appid, secret);
        String resp = HttpUtil.get(url, 5000);
        JSONObject json = JSONUtil.parseObj(resp);
        String token = json.getStr("access_token");
        if (token == null || token.isBlank()) {
            log.warn("wx access_token failed, appid={}, errcode={}, errmsg={}", maskAppid(appid), json.getInt("errcode"), json.getStr("errmsg"));
            throw new BusinessException(ErrorCode.BIND_PHONE_FAILED);
        }
        int expiresIn = json.getInt("expires_in", 7200);
        long ttl = Math.max(60, expiresIn - 60L);
        stringRedisTemplate.opsForValue().set(key, token, ttl, TimeUnit.SECONDS);
        return token;
    }

    private String maskAppid(String appid) {
        if (appid == null || appid.length() <= 8) {
            return "****";
        }
        return appid.substring(0, 4) + "****" + appid.substring(appid.length() - 4);
    }
}
