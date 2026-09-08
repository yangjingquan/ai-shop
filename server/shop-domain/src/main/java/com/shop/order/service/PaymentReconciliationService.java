package com.shop.order.service;

public interface PaymentReconciliationService {

    /** 主动核对待支付订单，并用支付回调的幂等入口补记已支付订单。 */
    int reconcilePending(int batchLimit);

    default int reconcilePending(int batchLimit, Long merchantId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return reconcilePending(batchLimit);
    }
}
