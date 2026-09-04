package com.shop.order.service;

/**
 * 微信侧不存在对应商户订单时使用，表示本地待支付订单没有可查询或关闭的支付单。
 */
public class WechatPayOrderNotFoundException extends RuntimeException {

    public WechatPayOrderNotFoundException(String orderNo, Throwable cause) {
        super("微信支付订单不存在: " + orderNo, cause);
    }
}
