package com.shop.order.job;

import com.shop.order.service.RefundReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.jobs", name = "refund-reconcile-enabled", havingValue = "true")
@RequiredArgsConstructor
public class RefundReconciliationJob {

    private final RefundReconciliationService refundReconciliationService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        int count = refundReconciliationService.reconcilePending(100);
        if (count > 0) {
            log.info("主动对账确认退款成功 {} 条", count);
        }
    }
}
