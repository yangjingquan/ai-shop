package com.shop.common.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiResultTest {
    @Test
    void successPayload() {
        ApiResult<String> r = ApiResult.success("hello");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMsg());
        assertEquals("hello", r.getData());
    }

    @Test
    void successNoPayload() {
        ApiResult<Void> r = ApiResult.success();
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void failurePayload() {
        ApiResult<Void> r = ApiResult.fail(110, "stock not enough");
        assertEquals(110, r.getCode());
        assertEquals("stock not enough", r.getMsg());
        assertNull(r.getData());
    }
}
