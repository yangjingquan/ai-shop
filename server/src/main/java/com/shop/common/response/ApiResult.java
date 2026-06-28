package com.shop.common.response;

import com.shop.common.exception.ErrorCode;
import lombok.Data;

@Data
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMsg());
    }
}
