package com.shop.order.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.aop.RateLimit;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.order.dto.*;
import com.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/order")
@RequiredArgsConstructor
public class WxOrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    public ApiResult<OrderPreviewVO> preview(@RequestBody @Valid OrderPreviewRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(orderService.preview(userId, req));
    }

    @RateLimit(key = "order_create", limit = 1, windowSec = 5, by = RateLimit.By.USER)
    @PostMapping("/create")
    public ApiResult<List<OrderCreateVO>> create(@RequestBody @Valid OrderCreateRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(orderService.create(userId, req));
    }

    @OpLog(action = "ORDER_CANCEL", targetType = "ORDER")
    @PostMapping("/{orderNo}/cancel")
    public ApiResult<Void> cancel(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        orderService.cancelByUser(userId, orderNo);
        return ApiResult.success(null);
    }

    @GetMapping("/page")
    public ApiResult<PageResult<OrderListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(orderService.page(userId, page, size, status));
    }

    @GetMapping("/{orderNo}")
    public ApiResult<OrderDetailVO> detail(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(orderService.detail(userId, orderNo));
    }

    @PostMapping("/{orderNo}/confirm-receive")
    public ApiResult<Void> confirmReceive(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        orderService.confirmReceive(userId, orderNo);
        return ApiResult.success(null);
    }

    @PostMapping("/{orderNo}/refund")
    public ApiResult<Void> refundApply(@PathVariable String orderNo, @RequestBody Map<String, String> body) {
        Long userId = CurrentUserHolder.get().getUserId();
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        orderService.refundApply(userId, orderNo, reason);
        return ApiResult.success(null);
    }

    @PostMapping("/{orderNo}/repay")
    public ApiResult<OrderCreateVO> repay(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(orderService.repay(userId, orderNo));
    }
}
