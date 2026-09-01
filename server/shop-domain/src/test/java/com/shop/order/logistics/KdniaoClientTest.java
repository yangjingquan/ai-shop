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
        assertEquals(Base64.getEncoder().encodeToString(new byte[]{
                0x62, (byte) 0x98, 0x27, (byte) 0x84, (byte) 0xac, 0x7a, 0x42, (byte) 0xee,
                0x7a, 0x51, 0x0c, 0x74, 0x0f, (byte) 0xe5, (byte) 0xae, (byte) 0x8d
        }), sign);
    }

    @Test
    void carrierResolverSupportsCodeAndLegacyName() {
        assertEquals("SF", LogisticsCarrier.resolve("SF", "").getCode());
        assertEquals("ZTO", LogisticsCarrier.resolve("", "中通快递").getCode());
        assertNull(LogisticsCarrier.resolve("UNKNOWN", "未知快递"));
    }
}
