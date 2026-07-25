package com.shop.groupbuy.job;

import com.shop.groupbuy.service.GroupBuyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyTimeoutJob {
    private final GroupBuyService groupBuyService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        int count = groupBuyService.failExpiredGroups(100);
        if (count > 0) {
            log.info("group buy timeout processed count={}", count);
        }
    }
}
