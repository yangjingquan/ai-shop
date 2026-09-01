package com.shop.order.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/** 快递鸟物流查询客户端。密钥只从运行时环境读取，不进入前端和源码。 */
@Slf4j
@Component
public class KdniaoClient {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String ebusinessId;
    private final String appKey;
    private final String queryUrl;
    private final String requestType;

    public KdniaoClient(ObjectMapper objectMapper,
                        RestTemplateBuilder restTemplateBuilder,
                        @Value("${shop.logistics.kdniao.ebusiness-id:}") String ebusinessId,
                        @Value("${shop.logistics.kdniao.app-key:}") String appKey,
                        @Value("${shop.logistics.kdniao.query-url:https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx}") String queryUrl,
                        @Value("${shop.logistics.kdniao.request-type:8002}") String requestType,
                        @Value("${shop.logistics.kdniao.timeout-ms:5000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.ebusinessId = ebusinessId == null ? "" : ebusinessId.trim();
        this.appKey = appKey == null ? "" : appKey.trim();
        this.queryUrl = queryUrl;
        this.requestType = normalizeRequestType(requestType);
    }

    public boolean configured() {
        return !ebusinessId.isBlank() && !appKey.isBlank();
    }

    public QueryResult query(String shipperCode, String logisticCode) {
        return query(shipperCode, logisticCode, "");
    }

    public QueryResult query(String shipperCode, String logisticCode, String customerName) {
        try {
            var request = objectMapper.createObjectNode();
            request.put("OrderCode", "");
            request.put("ShipperCode", shipperCode);
            request.put("LogisticCode", logisticCode);
            if (customerName != null && !customerName.isBlank()) request.put("CustomerName", customerName);
            String requestData = request.toString();

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("RequestType", requestType);
            form.add("EBusinessID", ebusinessId);
            form.add("RequestData", requestData);
            form.add("DataSign", dataSign(requestData, appKey));
            form.add("DataType", "2");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    queryUrl, new HttpEntity<>(form, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return QueryResult.failure("快递鸟接口返回异常");
            }

            JsonNode body = objectMapper.readTree(response.getBody());
            boolean success = body.path("Success").asBoolean(false);
            String reason = body.path("Reason").asText("");
            String state = body.path("State").asText("0");
            return new QueryResult(success, reason, state, body.path("Traces"));
        } catch (Exception e) {
            log.warn("快递鸟物流查询失败，承运商={}, 单号={}", shipperCode, logisticCode, e);
            return QueryResult.failure("物流查询暂时不可用，请稍后重试");
        }
    }

    static String dataSign(String requestData, String appKey) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest((requestData + appKey).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("无法生成快递鸟签名", e);
        }
    }

    static String normalizeRequestType(String value) {
        String normalized = value == null ? "" : value.trim();
        return "1002".equals(normalized) ? "1002" : "8002";
    }

    @Getter
    public static class QueryResult {
        private final boolean success;
        private final String reason;
        private final String state;
        private final JsonNode traces;

        public QueryResult(boolean success, String reason, String state, JsonNode traces) {
            this.success = success;
            this.reason = reason == null ? "" : reason;
            this.state = state == null || state.isBlank() ? "0" : state;
            this.traces = traces;
        }

        public static QueryResult failure(String reason) {
            return new QueryResult(false, reason, "0", null);
        }
    }
}
