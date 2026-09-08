package com.shop.order.service;

public interface RefundReconciliationService {

    /** 发起系统自动退款并主动查询微信退款结果。 */
    int reconcilePending(int batchLimit);

    default int reconcilePending(int batchLimit, Long merchantId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return reconcilePending(batchLimit);
    }
}
