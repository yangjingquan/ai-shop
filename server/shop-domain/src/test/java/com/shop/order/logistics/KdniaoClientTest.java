package com.shop.order.logistics;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class KdniaoClientTest {

    @Test
    void defaultsToPaidExpressQueryApi() {
        assertEquals("8002", KdniaoClient.normalizeRequestType(null));
        assertEquals("8002", KdniaoClient.normalizeRequestType("8002"));
        assertEquals("1002", KdniaoClient.normalizeRequestType("1002"));
        assertEquals("8002", KdniaoClient.normalizeRequestType("unsupported"));
    }

    @Test
    void dataSignUsesBase64Md5OfRequestDataAndAppKey() {
        String sign = KdniaoClient.dataSign("{}", "key");
        assertEquals("NjI5ODI3ODRhYzdhNDJlZTdhNTEwYzc0MGZlNWFlOGQ=", sign);
    }

    @Test
    void carrierResolverSupportsCodeAndLegacyName() {
        assertEquals("SF", LogisticsCarrier.resolve("SF", "").getCode());
        assertEquals("ZTO", LogisticsCarrier.resolve("", "中通快递").getCode());
        assertNull(LogisticsCarrier.resolve("UNKNOWN", "未知快递"));
    }
}
