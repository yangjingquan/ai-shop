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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), "请求参数格式错误"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), "图片大小不能超过 10MB"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResult<Void>> handleMultipart(MultipartException e) {
        log.warn("文件上传请求解析失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), "图片上传格式错误"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage()));
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
