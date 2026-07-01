package com.shop.order.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.jobs", name = "auto-receive-enabled", havingValue = "true")
@RequiredArgsConstructor
public class AutoReceiveJob {

    private final OrderMapper orderMapper;

    @Scheduled(fixedDelay = 600_000) // 每 10 分钟扫一次
    @Transactional
    public void run() {
        log.debug("开始扫描超时未收货订单");
        List<Order> expired = orderMapper.selectAutoReceiveOrders(100);
        if (expired.isEmpty()) return;
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Order order : expired) {
            try {
                orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .eq(Order::getStatus, OrderStatus.WAIT_RECEIVE.getCode())
                        .set(Order::getStatus, OrderStatus.FINISHED.getCode())
                        .set(Order::getFinishTime, now)
                        .set(Order::getUpdatedAt, now));
                count++;
            } catch (Exception e) {
                log.error("超时自动确认收货失败 orderNo={}", order.getOrderNo(), e);
            }
        }
        if (count > 0) {
            log.info("超时自动确认收货 {} 条", count);
        }
    }
}
