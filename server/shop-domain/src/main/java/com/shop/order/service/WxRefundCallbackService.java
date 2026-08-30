package com.shop.order.service;

public interface WxRefundCallbackService {
    void handle(String merchantCode, WxPayCallbackService.WxPayCallbackHeaders headers, String rawBody);
}
