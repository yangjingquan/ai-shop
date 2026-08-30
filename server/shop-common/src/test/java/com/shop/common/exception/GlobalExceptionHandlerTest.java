package com.shop.common.exception;

import com.shop.common.response.ApiResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        ResponseEntity<ApiResult<Void>> response = handler.handleBusiness(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResult<Void> r = response.getBody();
        assertNotNull(r);
        assertEquals(110, r.getCode());
        assertEquals("库存不足", r.getMsg());
    }

    @Test
    void handleUnknownException() {
        ResponseEntity<ApiResult<Void>> response = handler.handleUnknown(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResult<Void> r = response.getBody();
        assertNotNull(r);
        assertEquals(500, r.getCode());
        assertEquals("系统错误", r.getMsg());
    }
}
