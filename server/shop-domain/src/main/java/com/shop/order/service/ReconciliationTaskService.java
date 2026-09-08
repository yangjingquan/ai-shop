package com.shop.order.service;

import com.shop.order.dto.ReconciliationTaskVO;
import java.time.LocalDateTime;
import java.util.List;

public interface ReconciliationTaskService {
    ReconciliationTaskVO preview(String type, Long merchantId, LocalDateTime from, LocalDateTime to);
    ReconciliationTaskVO createAndRun(String type, Long merchantId, LocalDateTime from, LocalDateTime to, String requestedBy);
    List<ReconciliationTaskVO> recent(String type, int limit);
}
