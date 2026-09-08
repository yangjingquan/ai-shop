package com.shop.inventory.service;

/**
 * The single durable boundary for order-bound stock and entitlement transitions.
 * Repeating any method is safe: only the expected previous state can move.
 */
public interface ResourceReservationService {
    void reserveSku(String orderNo, Long merchantId, Long productId, Long skuId, int quantity, String reason);
    void reserveMarker(String orderNo, Long merchantId, String resourceType, String resourceId, int quantity, String reason);
    void confirmOrder(String orderNo);
    void releaseOrder(String orderNo, String reason);
    void restockConfirmedOrder(String orderNo, String reason);
}
