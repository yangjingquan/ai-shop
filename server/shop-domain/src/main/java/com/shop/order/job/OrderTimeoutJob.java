package com.shop.order.job;

import com.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.jobs", name = "order-timeout-enabled", havingValue = "true")
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        log.debug("开始扫描过期订单");
        int count = orderService.cancelExpired(100);
        if (count > 0) {
            log.info("取消过期订单 {} 条", count);
        }
    }
}
