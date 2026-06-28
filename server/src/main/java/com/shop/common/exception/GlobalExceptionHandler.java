package com.shop.common.exception;

import com.shop.common.response.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleValidation(Exception e) {
        String msg = "参数错误";
        if (e instanceof MethodArgumentNotValidException ex) {
            msg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().iterator().next().getMessage();
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleUnknown(Exception e) {
        log.error("未知异常", e);
        return ApiResult.fail(ErrorCode.SYSTEM_ERROR);
    }
}
