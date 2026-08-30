package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean receiveIfWaiting(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        return orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.WAIT_RECEIVE.getCode())
                .set(Order::getStatus, OrderStatus.FINISHED.getCode())
                .set(Order::getFinishTime, now)
                .set(Order::getUpdatedAt, now)) == 1;
    }
}
