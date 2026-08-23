package com.shop.order.service;

public interface WxPayCallbackService {
    void handle(String merchantCode, WxPayCallbackHeaders headers, String rawBody);

    record WxPayCallbackHeaders(
            String timestamp,
            String nonce,
            String signature,
            String serial
    ) {}
}
