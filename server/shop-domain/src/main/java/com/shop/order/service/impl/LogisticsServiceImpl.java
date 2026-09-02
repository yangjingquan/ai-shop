package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.dto.LogisticsTrackingVO;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderLogisticsTrace;
import com.shop.order.enums.OrderStatus;
import com.shop.order.logistics.KdniaoClient;
import com.shop.order.logistics.LogisticsCarrier;
import com.shop.order.mapper.OrderLogisticsTraceMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private static final DateTimeFormatter TRACE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_ERROR_LENGTH = 500;

    private final OrderMapper orderMapper;
    private final OrderLogisticsTraceMapper traceMapper;
    private final KdniaoClient kdniaoClient;
    private final ObjectMapper objectMapper;

    @Value("${shop.logistics.enabled:true}")
    private boolean enabled;

    @Value("${shop.logistics.cache-minutes:30}")
    private long cacheMinutes;

    @Override
    @Transactional
    public LogisticsTrackingVO trackForUser(Long userId, String orderNo, boolean forceRefresh) {
        return track(findOrder(orderNo, OrderScope.USER, userId), forceRefresh);
    }

    @Override
    @Transactional
    public LogisticsTrackingVO trackForMerchant(Long merchantId, String orderNo, boolean forceRefresh) {
        return track(findOrder(orderNo, OrderScope.MERCHANT, merchantId), forceRefresh);
    }

    @Override
    @Transactional
    public LogisticsTrackingVO trackForAdmin(String orderNo, boolean forceRefresh) {
        return track(findOrder(orderNo, OrderScope.ADMIN, null), forceRefresh);
    }

    private Order findOrder(String orderNo, OrderScope scope, Long ownerId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo);
        if (scope == OrderScope.USER) wrapper.eq(Order::getUserId, ownerId);
        if (scope == OrderScope.MERCHANT) wrapper.eq(Order::getMerchantId, ownerId);
        return orderMapper.selectOne(wrapper);
    }

    private LogisticsTrackingVO track(Order order, boolean forceRefresh) {
        if (order == null) return empty("订单不存在");
        LogisticsTrackingVO view = fromOrder(order);
        if (order.getShipNo() == null || order.getShipNo().isBlank()) {
            view.setError("商家尚未录入物流单号");
            return view;
        }

        List<OrderLogisticsTrace> stored = loadTraces(order.getOrderNo());
        if (!forceRefresh && isCacheFresh(order.getLogisticsSyncedAt())) {
            view.setTraces(toTraceViews(stored));
            return view;
        }
        if (!enabled || !kdniaoClient.configured()) {
            view.setTraces(toTraceViews(stored));
            view.setError("物流查询服务暂未配置");
            return view;
        }

        LogisticsCarrier carrier = LogisticsCarrier.resolve(order.getShipperCode(), order.getShipCompany());
        if (carrier == null) {
            view.setTraces(toTraceViews(stored));
            view.setError("暂不支持该物流公司，请联系商家补充承运商编码");
            return view;
        }

        // “邮政”订单的实际渠道可能是 EMS 或 YZPY。让快递鸟按单号识别，避免
        // 用 YZPY 强制查询一个实际属于 EMS 的单号而返回“非法参数”。
        String queryShipperCode = carrier == LogisticsCarrier.YZPY ? "" : carrier.getCode();
        KdniaoClient.QueryResult result = kdniaoClient.query(queryShipperCode, order.getShipNo(), customerName(order, carrier));
        LocalDateTime syncedAt = LocalDateTime.now();
        if (!result.isSuccess()) {
            updateSummary(order, syncedAt, result.getReason());
            view = fromOrder(order);
            view.setSyncedAt(syncedAt);
            view.setError(trim(result.getReason()));
            view.setTraces(toTraceViews(stored));
            return view;
        }

        String resolvedCarrierCode = resolveCarrierCode(result.getShipperCode(), carrier.getCode());
        persistTraces(order, resolvedCarrierCode, result);
        List<OrderLogisticsTrace> latest = loadTraces(order.getOrderNo());
        OrderLogisticsTrace last = latest.stream().max(Comparator.comparing(OrderLogisticsTrace::getAcceptTime)).orElse(null);
        String stateText = stateText(result.getState());
        updateSummary(order, syncedAt, "");
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, order.getOrderNo())
                .set(Order::getShipperCode, resolvedCarrierCode)
                .set(Order::getLogisticsState, result.getState())
                .set(Order::getLogisticsStateText, stateText)
                .set(Order::getLogisticsLastTime, last == null ? null : last.getAcceptTime())
                .set(Order::getLogisticsLastContent, last == null ? "" : last.getAcceptStation())
                .set(Order::getLogisticsSyncedAt, syncedAt)
                .set(Order::getLogisticsError, ""));
        order.setShipperCode(resolvedCarrierCode);
        order.setLogisticsState(result.getState());
        order.setLogisticsStateText(stateText);
        order.setLogisticsLastTime(last == null ? null : last.getAcceptTime());
        order.setLogisticsLastContent(last == null ? "" : last.getAcceptStation());
        order.setLogisticsSyncedAt(syncedAt);
        order.setLogisticsError("");
        view = fromOrder(order);
        view.setTraces(toTraceViews(latest));
        return view;
    }

    private void persistTraces(Order order, String carrierCode, KdniaoClient.QueryResult result) {
        JsonNode traces = result.getTraces();
        if (traces == null || !traces.isArray()) return;
        for (JsonNode node : traces) {
            String station = node.path("AcceptStation").asText("").trim();
            LocalDateTime acceptTime = parseTime(node.path("AcceptTime").asText(""));
            if (station.isBlank() || acceptTime == null) continue;
            String hash = sha256(acceptTime + "|" + station);
            boolean exists = traceMapper.selectCount(new LambdaQueryWrapper<OrderLogisticsTrace>()
                    .eq(OrderLogisticsTrace::getOrderNo, order.getOrderNo())
                    .eq(OrderLogisticsTrace::getTraceHash, hash)) > 0;
            if (exists) continue;
            OrderLogisticsTrace trace = new OrderLogisticsTrace();
            trace.setOrderNo(order.getOrderNo());
            trace.setShipperCode(carrierCode);
            trace.setLogisticCode(order.getShipNo());
            trace.setState(result.getState());
            trace.setAcceptTime(acceptTime);
            trace.setAcceptStation(station);
            trace.setTraceHash(hash);
            traceMapper.insert(trace);
        }
    }

    private String customerName(Order order, LogisticsCarrier carrier) {
        if (carrier != LogisticsCarrier.SF || order.getAddressSnapshot() == null) return "";
        try {
            String phone = objectMapper.readTree(order.getAddressSnapshot()).path("phone").asText("");
            String digits = phone.replaceAll("\\D", "");
            return digits.length() >= 4 ? digits.substring(digits.length() - 4) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveCarrierCode(String returnedCode, String fallbackCode) {
        LogisticsCarrier returned = LogisticsCarrier.resolve(returnedCode, "");
        return returned == null ? fallbackCode : returned.getCode();
    }

    private void updateSummary(Order order, LocalDateTime syncedAt, String error) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, order.getOrderNo())
                .set(Order::getLogisticsSyncedAt, syncedAt)
                .set(Order::getLogisticsError, trim(error)));
        order.setLogisticsSyncedAt(syncedAt);
        order.setLogisticsError(trim(error));
    }

    private List<OrderLogisticsTrace> loadTraces(String orderNo) {
        return traceMapper.selectList(new LambdaQueryWrapper<OrderLogisticsTrace>()
                .eq(OrderLogisticsTrace::getOrderNo, orderNo)
                .orderByDesc(OrderLogisticsTrace::getAcceptTime));
    }

    private LogisticsTrackingVO fromOrder(Order order) {
        LogisticsTrackingVO view = new LogisticsTrackingVO();
        view.setOrderNo(order.getOrderNo());
        view.setShipCompany(order.getShipCompany());
        view.setShipperCode(order.getShipperCode());
        view.setShipNo(order.getShipNo());
        view.setState(defaultValue(order.getLogisticsState(), "0"));
        view.setStateText(defaultValue(order.getLogisticsStateText(), stateText(view.getState())));
        view.setLastTime(order.getLogisticsLastTime());
        view.setLastContent(order.getLogisticsLastContent());
        view.setSyncedAt(order.getLogisticsSyncedAt());
        view.setError(defaultValue(order.getLogisticsError(), ""));
        view.setTraces(new ArrayList<>());
        return view;
    }

    private List<LogisticsTrackingVO.Trace> toTraceViews(List<OrderLogisticsTrace> traces) {
        return traces.stream().map(trace -> {
            LogisticsTrackingVO.Trace view = new LogisticsTrackingVO.Trace();
            view.setAcceptTime(trace.getAcceptTime());
            view.setAcceptStation(trace.getAcceptStation());
            view.setState(trace.getState());
            view.setStateText(stateText(trace.getState()));
            return view;
        }).toList();
    }

    private boolean isCacheFresh(LocalDateTime syncedAt) {
        return syncedAt != null && syncedAt.isAfter(LocalDateTime.now().minusMinutes(Math.max(cacheMinutes, 1)));
    }

    private LocalDateTime parseTime(String value) {
        try { return LocalDateTime.parse(value, TRACE_TIME); }
        catch (DateTimeParseException e) { return null; }
    }

    private String stateText(String state) {
        return switch (state) {
            case "1" -> "已揽收";
            case "2" -> "运输中";
            case "3" -> "已签收";
            case "4" -> "问题件";
            default -> "暂无轨迹";
        };
    }

    private String defaultValue(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private String trim(String value) {
        if (value == null) return "";
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }

    private LogisticsTrackingVO empty(String error) {
        LogisticsTrackingVO view = new LogisticsTrackingVO();
        view.setError(error);
        view.setTraces(new ArrayList<>());
        return view;
    }

    private String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成物流轨迹指纹", e);
        }
    }

    private enum OrderScope { USER, MERCHANT, ADMIN }
}
