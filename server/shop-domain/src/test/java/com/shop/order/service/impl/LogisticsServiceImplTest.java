package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.order.dto.LogisticsTrackingVO;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderLogisticsTrace;
import com.shop.order.logistics.KdniaoClient;
import com.shop.order.mapper.OrderLogisticsTraceMapper;
import com.shop.order.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogisticsServiceImplTest {

    @Test
    void freshCacheDoesNotCallThirdParty() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderLogisticsTraceMapper traceMapper = mock(OrderLogisticsTraceMapper.class);
        KdniaoClient client = mock(KdniaoClient.class);
        Order order = new Order();
        order.setOrderNo("ORDER_CACHE");
        order.setUserId(7L);
        order.setShipCompany("顺丰");
        order.setShipperCode("SF");
        order.setShipNo("SF12345678");
        order.setLogisticsState("2");
        order.setLogisticsStateText("运输中");
        order.setLogisticsSyncedAt(LocalDateTime.now());
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(traceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        LogisticsServiceImpl service = new LogisticsServiceImpl(orderMapper, traceMapper, client, new ObjectMapper());
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheMinutes", 30L);

        LogisticsTrackingVO result = service.trackForUser(7L, "ORDER_CACHE", false);

        assertEquals("运输中", result.getStateText());
        assertEquals("SF12345678", result.getShipNo());
        verify(client, never()).query(anyString(), anyString());
        verify(traceMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
