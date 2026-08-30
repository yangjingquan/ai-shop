package com.shop.order.job;

import com.shop.order.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.jobs", name = "payment-reconcile-enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentReconciliationService paymentReconciliationService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        int paidCount = paymentReconciliationService.reconcilePending(100);
        if (paidCount > 0) {
            log.info("主动查单补记已支付订单 {} 条", paidCount);
        }
    }
}
