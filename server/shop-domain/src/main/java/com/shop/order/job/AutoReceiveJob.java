package com.shop.order.job;

import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.AutoReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.jobs", name = "auto-receive-enabled", havingValue = "true")
@RequiredArgsConstructor
public class AutoReceiveJob {

    private final OrderMapper orderMapper;
    private final AutoReceiveService autoReceiveService;

    @Scheduled(fixedDelay = 600_000) // 每 10 分钟扫一次
    public void run() {
        log.debug("开始扫描超时未收货订单");
        List<Order> expired = orderMapper.selectAutoReceiveOrders(100);
        if (expired.isEmpty()) return;
        int count = 0;
        for (Order order : expired) {
            try {
                if (autoReceiveService.receiveIfWaiting(order.getId())) {
                    count++;
                }
            } catch (Exception e) {
                log.error("超时自动确认收货失败 orderNo={}", order.getOrderNo(), e);
            }
        }
        if (count > 0) {
            log.info("超时自动确认收货 {} 条", count);
        }
    }
}
