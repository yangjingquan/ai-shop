package com.shop.order.service;

/** 单个订单自动收货，失败不影响同批其他订单。 */
public interface AutoReceiveService {
    boolean receiveIfWaiting(Long orderId);
}
