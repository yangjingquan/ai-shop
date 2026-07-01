package com.shop.order.service;

public interface OrderPaymentService {

    /** 支付回调处理（mock 和真回调共用）。幂等。 */
    void handlePaidCallback(String orderNo, String transactionId, String rawPayload);
}
