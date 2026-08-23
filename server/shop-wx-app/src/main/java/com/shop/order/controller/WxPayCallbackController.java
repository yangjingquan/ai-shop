package com.shop.order.controller;

import com.shop.order.service.WxPayCallbackService;
import com.shop.order.service.WxPayCallbackService.WxPayCallbackHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class WxPayCallbackController {

    private final WxPayCallbackService wxPayCallbackService;

    @PostMapping("/wxpay/{merchantCode}")
    public Map<String, String> wxpayCallback(@PathVariable String merchantCode,
                                             @RequestHeader("Wechatpay-Timestamp") String timestamp,
                                             @RequestHeader("Wechatpay-Nonce") String nonce,
                                             @RequestHeader("Wechatpay-Signature") String signature,
                                             @RequestHeader("Wechatpay-Serial") String serial,
                                             @RequestBody String rawBody) {
        wxPayCallbackService.handle(merchantCode, new WxPayCallbackHeaders(timestamp, nonce, signature, serial), rawBody);
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    @PostMapping("/wxpay")
    public Map<String, String> wxpayCallbackWithoutMerchant() {
        return Map.of("code", "FAIL", "message", "缺少商户代码");
    }
}
