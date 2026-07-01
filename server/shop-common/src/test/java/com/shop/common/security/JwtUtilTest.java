package com.shop.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setField("secret", "test-secret-key-must-be-at-least-32-bytes-long!!");
        setField("adminTtlHours", 8);
        setField("merchantTtlHours", 8);
        setField("wxTtlDays", 7);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = JwtUtil.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(jwtUtil, value);
    }

    @Test
    void generateAndParseAdminToken() {
        String token = jwtUtil.generateToken(UserType.ADMIN, Map.of("userId", 1L));
        assertTrue(jwtUtil.isValid(token));
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("ADMIN", claims.get("userType"));
        assertEquals(1, ((Number) claims.get("userId")).intValue());
    }

    @Test
    void generateMerchantToken() {
        String token = jwtUtil.generateToken(UserType.MERCHANT, Map.of("userId", 2L, "merchantId", 100L));
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("MERCHANT", claims.get("userType"));
        assertEquals(100, ((Number) claims.get("merchantId")).intValue());
    }

    @Test
    void generateWxToken() {
        String token = jwtUtil.generateToken(UserType.WX, Map.of("userId", 3L, "openid", "ox123"));
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("WX", claims.get("userType"));
        assertEquals("ox123", claims.get("openid"));
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertFalse(jwtUtil.isValid("garbage.token.here"));
    }

    @Test
    void tamperedTokenReturnsFalse() {
        String token = jwtUtil.generateToken(UserType.ADMIN, Map.of("userId", 1L));
        // 翻转签名段中间一个字符，确保签名校验失败
        int dotIdx = token.lastIndexOf('.');
        char c = token.charAt(dotIdx + 5);
        char flipped = (c == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, dotIdx + 5) + flipped + token.substring(dotIdx + 6);
        assertFalse(jwtUtil.isValid(tampered));
    }
}
