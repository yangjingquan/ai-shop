package com.shop.common.exception;

import com.shop.common.response.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(resolveStatus(e.getCode()))
                .body(ApiResult.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<Void>> handleValidation(Exception e) {
        String msg = "参数错误";
        if (e instanceof MethodArgumentNotValidException ex) {
            msg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().iterator().next().getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnknown(Exception e) {
        log.error("未知异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCode.SYSTEM_ERROR));
    }

    private HttpStatus resolveStatus(int code) {
        if (code == HttpStatus.UNAUTHORIZED.value()) return HttpStatus.UNAUTHORIZED;
        if (code == HttpStatus.FORBIDDEN.value()) return HttpStatus.FORBIDDEN;
        if (code == HttpStatus.TOO_MANY_REQUESTS.value()) return HttpStatus.TOO_MANY_REQUESTS;
        if (code >= 500) return HttpStatus.INTERNAL_SERVER_ERROR;
        return HttpStatus.BAD_REQUEST;
    }
}
