package com.shop.journey.job;

import com.shop.journey.service.MarketingJourneyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically turns audience signals into due marketing actions. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingJourneyJob {
    private final MarketingJourneyService marketingJourneyService;

    @Scheduled(fixedDelay = 300_000)
    public void execute() {
        try {
            marketingJourneyService.scanAll();
            marketingJourneyService.processDue();
        } catch (RuntimeException ex) {
            log.error("自动营销调度失败", ex);
        }
    }
}
