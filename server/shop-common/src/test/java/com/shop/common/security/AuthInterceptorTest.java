package com.shop.common.security;

import com.shop.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthInterceptorTest {

    private JwtUtil jwtUtil;
    private AdminAuthInterceptor adminInterceptor;
    private MerchantAuthInterceptor merchantInterceptor;
    private WxAuthInterceptor wxInterceptor;

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        jwtUtil = new JwtUtil();
        Field f = JwtUtil.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(jwtUtil, "test-secret-key-must-be-at-least-32-bytes-long!!");
        for (String field : new String[]{"adminTtlHours", "merchantTtlHours"}) {
            Field ff = JwtUtil.class.getDeclaredField(field);
            ff.setAccessible(true);
            ff.set(jwtUtil, 8);
        }
        Field wf = JwtUtil.class.getDeclaredField("wxTtlDays");
        wf.setAccessible(true);
        wf.set(jwtUtil, 7);

        adminInterceptor = new AdminAuthInterceptor(jwtUtil);
        merchantInterceptor = new MerchantAuthInterceptor(jwtUtil);
        wxInterceptor = new WxAuthInterceptor(jwtUtil);
    }

    @Test
    void adminInterceptorAcceptsValidAdminToken() {
        String token = jwtUtil.generateToken(UserType.ADMIN, Map.of("userId", 1L));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        assertTrue(adminInterceptor.preHandle(request, response, new Object()));
        assertEquals(UserType.ADMIN, CurrentUserHolder.get().getUserType());
        CurrentUserHolder.clear();
    }

    @Test
    void adminInterceptorRejectsMerchantToken() {
        String token = jwtUtil.generateToken(UserType.MERCHANT, Map.of("userId", 2L, "merchantId", 10L));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        assertThrows(BusinessException.class, () -> adminInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void adminInterceptorRejectsNoHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertThrows(BusinessException.class, () -> adminInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void merchantInterceptorAcceptsValidMerchantToken() {
        String token = jwtUtil.generateToken(UserType.MERCHANT, Map.of("userId", 2L, "merchantId", 10L));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        assertTrue(merchantInterceptor.preHandle(request, response, new Object()));
        assertEquals(10L, CurrentUserHolder.get().getMerchantId());
        CurrentUserHolder.clear();
    }

    @Test
    void wxInterceptorAcceptsValidWxToken() {
        String token = jwtUtil.generateToken(UserType.WX, Map.of("userId", 3L, "openid", "ox123"));
        when(request.getHeader("wx-token")).thenReturn(token);
        assertTrue(wxInterceptor.preHandle(request, response, new Object()));
        assertEquals(UserType.WX, CurrentUserHolder.get().getUserType());
        CurrentUserHolder.clear();
    }

    @Test
    void wxInterceptorRejectsNoHeader() {
        when(request.getHeader("wx-token")).thenReturn(null);
        assertThrows(BusinessException.class, () -> wxInterceptor.preHandle(request, response, new Object()));
    }
}
