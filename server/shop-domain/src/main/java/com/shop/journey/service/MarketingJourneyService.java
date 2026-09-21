package com.shop.journey.service;
import com.shop.journey.dto.*; import java.util.List;
public interface MarketingJourneyService { List<MarketingJourneyVO> list(Long merchantId); Long save(Long merchantId, Long id, MarketingJourneyRequest request); void scan(Long merchantId, Long journeyId); void scanAll(); void processDue(); }
