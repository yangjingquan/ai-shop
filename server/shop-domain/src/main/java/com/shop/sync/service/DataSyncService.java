package com.shop.sync.service;

import com.shop.sync.dto.DataVersionVO;

public interface DataSyncService {
    void invalidate(String scope, Long merchantId, String resourceId, String action);
    DataVersionVO versionSince(Long merchantId, Long knownVersion);
}
