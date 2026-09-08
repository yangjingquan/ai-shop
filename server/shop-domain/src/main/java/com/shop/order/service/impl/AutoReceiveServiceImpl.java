package com.shop.order.service.impl;

import com.shop.order.enums.OrderStatus;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderStateMachine;
import com.shop.order.service.AutoReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutoReceiveServiceImpl implements AutoReceiveService {

    private final OrderMapper orderMapper;
    private final OrderStateMachine orderStateMachine;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean receiveIfWaiting(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != OrderStatus.WAIT_RECEIVE.getCode()) return false;
        order.setFinishTime(LocalDateTime.now());
        orderStateMachine.transition(order, OrderStatus.FINISHED, "AUTO_RECEIVED", "超时自动收货");
        return true;
    }
}
