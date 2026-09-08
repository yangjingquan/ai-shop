package com.shop.sync.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.sync.dto.DataVersionVO;
import com.shop.sync.entity.DataSyncEvent;
import com.shop.sync.mapper.DataSyncEventMapper;
import com.shop.sync.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSyncServiceImpl implements DataSyncService {
    private final DataSyncEventMapper eventMapper;

    @Override
    public void invalidate(String scope, Long merchantId, String resourceId, String action) {
        DataSyncEvent event = new DataSyncEvent();
        event.setScope(scope == null ? "ORDER" : scope);
        event.setMerchantId(merchantId);
        event.setResourceId(resourceId == null ? "" : resourceId);
        event.setAction(action == null ? "INVALIDATE" : action);
        eventMapper.insert(event);
    }

    @Override
    public DataVersionVO versionSince(Long merchantId, Long knownVersion) {
        long current = eventMapper.selectList(new LambdaQueryWrapper<DataSyncEvent>()
                        .orderByDesc(DataSyncEvent::getId).last("LIMIT 1"))
                .stream().findFirst().map(DataSyncEvent::getId).orElse(0L);
        List<String> scopes = eventMapper.selectList(new LambdaQueryWrapper<DataSyncEvent>()
                        .gt(DataSyncEvent::getId, knownVersion == null ? 0L : knownVersion)
                        .and(merchantId != null, q -> q.eq(DataSyncEvent::getMerchantId, merchantId).or().isNull(DataSyncEvent::getMerchantId))
                        .orderByAsc(DataSyncEvent::getId).last("LIMIT 100"))
                .stream().map(DataSyncEvent::getScope).distinct().toList();
        return new DataVersionVO(current, LocalDateTime.now(), scopes);
    }
}
