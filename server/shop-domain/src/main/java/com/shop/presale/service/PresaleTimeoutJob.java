package com.shop.presale.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.jobs", name = "presale-timeout-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PresaleTimeoutJob {
    private final PresaleService presaleService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        int count = presaleService.expireBalanceOrders(100);
        if (count > 0) log.info("处理预售尾款逾期订单 {} 条", count);
    }
}
