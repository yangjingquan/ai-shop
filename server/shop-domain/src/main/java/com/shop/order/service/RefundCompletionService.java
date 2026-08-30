package com.shop.order.service;

import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;

import java.time.LocalDateTime;

public interface RefundCompletionService {

    /** 完成全额退款后的订单及拼团成员状态闭环。 */
    void completeIfFullRefund(RefundApplication refund, Order order, LocalDateTime completedAt);
}
