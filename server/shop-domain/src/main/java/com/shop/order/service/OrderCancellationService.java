package com.shop.order.service;

/**
 * 将取消订单放到独立事务中执行，避免定时任务的批量事务或支付查单与库存回滚互相影响。
 */
public interface OrderCancellationService {

    void cancelByUser(Long userId, String orderNo);

    boolean cancelExpired(Long orderId);
}
