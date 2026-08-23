package com.shop.order.service;

public interface OrderPaymentService {

    /** 已通过微信验签和金额校验的支付成功通知处理。幂等。 */
    void handlePaidCallback(String orderNo, String transactionId, String rawPayload);
}
