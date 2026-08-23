package com.shop.order.controller;

import com.shop.order.service.WxPayCallbackService;
import com.shop.order.service.WxPayCallbackService.WxPayCallbackHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class WxPayCallbackController {

    private final WxPayCallbackService wxPayCallbackService;

    @PostMapping("/wxpay/{merchantCode}")
    public ResponseEntity<Map<String, String>> wxpayCallback(@PathVariable String merchantCode,
                                                              @RequestHeader Map<String, String> headers,
                                                              @RequestBody(required = false) String rawBody) {
        try {
            wxPayCallbackService.handle(merchantCode, new WxPayCallbackHeaders(
                    header(headers, "Wechatpay-Timestamp"), header(headers, "Wechatpay-Nonce"),
                    header(headers, "Wechatpay-Signature"), header(headers, "Wechatpay-Serial")), rawBody == null ? "" : rawBody);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception e) {
            // 微信仅在收到 2xx 时停止重试；不要让全局业务异常处理器将验签失败改为 200。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "通知处理失败"));
        }
    }

    @PostMapping("/wxpay")
    public ResponseEntity<Map<String, String>> wxpayCallbackWithoutMerchant() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "FAIL", "message", "缺少商户代码"));
    }

    private String header(Map<String, String> headers, String name) {
        String value = headers.get(name);
        if (value == null || value.isBlank()) {
            value = headers.get(name.toLowerCase());
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing callback header");
        }
        return value;
    }
}
