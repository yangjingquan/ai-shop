package com.shop.common.exception;

import com.shop.common.response.ApiResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        ApiResult<Void> r = handler.handleBusiness(ex);
        assertEquals(110, r.getCode());
        assertEquals("库存不足", r.getMsg());
    }

    @Test
    void handleUnknownException() {
        ApiResult<Void> r = handler.handleUnknown(new RuntimeException("boom"));
        assertEquals(500, r.getCode());
        assertEquals("系统错误", r.getMsg());
    }
}
