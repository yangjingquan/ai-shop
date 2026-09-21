package com.shop.customer.job;

import com.shop.customer.service.CustomerOperationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerMetricSnapshotJob {
    private final CustomerOperationsService customerOperationsService;

    @Scheduled(cron = "0 10 2 * * ?")
    public void refreshDaily() {
        try {
            customerOperationsService.refreshAllMetrics();
        } catch (RuntimeException ex) {
            log.error("用户运营指标快照刷新失败", ex);
        }
    }
}
