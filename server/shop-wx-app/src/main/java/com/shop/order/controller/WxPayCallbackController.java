package com.shop.order.controller;

import com.shop.order.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class WxPayCallbackController {

    private final OrderPaymentService orderPaymentService;

    /** 微信支付回调入口。M4a 空壳，M4b 接真微信支付时补验签+解密逻辑。 */
    @PostMapping("/wxpay")
    public String wxpayCallback(@RequestBody String rawBody) {
        // M4a: 不解析，留给 M4b
        return "SUCCESS";
    }
}
