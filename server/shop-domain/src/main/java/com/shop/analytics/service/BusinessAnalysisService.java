package com.shop.analytics.service;
import com.shop.analytics.dto.BusinessAnalysisVO;
public interface BusinessAnalysisService {
    void recordProductView(Long merchantId, Long userId, String eventId, Long productId, String channelCode);
    void snapshotOrder(Long merchantId, String orderNo);
    BusinessAnalysisVO report(Long merchantId, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
