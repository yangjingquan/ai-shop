package com.shop.order.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.aop.RateLimit;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import com.shop.order.dto.*;
import com.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wx/order")
@RequiredArgsConstructor
public class WxOrderController {

    private final OrderService orderService;
    private final com.shop.order.service.LogisticsService logisticsService;
    private final WxMerchantResolver wxMerchantResolver;

    @PostMapping("/preview")
    public ApiResult<OrderPreviewVO> preview(@RequestBody @Valid OrderPreviewRequest req, HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        return ApiResult.success(orderService.preview(userId, merchantId, req));
    }

    @RateLimit(key = "order_create", limit = 1, windowSec = 5, by = RateLimit.By.USER)
    @PostMapping("/create")
    public ApiResult<List<OrderCreateVO>> create(@RequestBody @Valid OrderCreateRequest req, HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        return ApiResult.success(orderService.create(userId, merchantId, req));
    }

    @OpLog(action = "ORDER_CANCEL", targetType = "ORDER", targetIdExpr = "#orderNo")
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

    @GetMapping("/{orderNo}/logistics")
    public ApiResult<LogisticsTrackingVO> logistics(@PathVariable String orderNo) {
        return ApiResult.success(logisticsService.trackForUser(
                CurrentUserHolder.get().getUserId(), orderNo, false));
    }

    @RateLimit(key = "logistics_refresh", limit = 1, windowSec = 60, by = RateLimit.By.USER)
    @PostMapping("/{orderNo}/logistics/refresh")
    public ApiResult<LogisticsTrackingVO> refreshLogistics(@PathVariable String orderNo) {
        return ApiResult.success(logisticsService.trackForUser(
                CurrentUserHolder.get().getUserId(), orderNo, true));
    }

    @PostMapping("/{orderNo}/confirm-receive")
    public ApiResult<Void> confirmReceive(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        orderService.confirmReceive(userId, orderNo);
        return ApiResult.success(null);
    }

    @OpLog(action = "ORDER_REMIND_SHIP", targetType = "ORDER", targetIdExpr = "#orderNo")
    @PostMapping("/{orderNo}/remind-ship")
    public ApiResult<Void> remindShip(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        orderService.remindShip(userId, orderNo);
        return ApiResult.success(null);
    }

    @RateLimit(key = "refund_apply", limit = 3, windowSec = 60, by = RateLimit.By.USER)
    @PostMapping("/{orderNo}/refund")
    public ApiResult<Void> refundApply(@PathVariable String orderNo, @RequestBody(required = false) @Valid RefundApplyRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        orderService.refundApply(userId, orderNo, req == null ? new RefundApplyRequest() : req);
        return ApiResult.success(null);
    }

    @PostMapping("/refund/{refundId}/return-shipment")
    public ApiResult<Void> submitReturnShipment(@PathVariable Long refundId,
                                                @RequestBody @Valid ReturnShipmentRequest req) {
        orderService.submitReturnShipment(CurrentUserHolder.get().getUserId(), refundId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{orderNo}/repay")
    public ApiResult<OrderCreateVO> repay(@PathVariable String orderNo, HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        return ApiResult.success(orderService.repay(userId, merchantId, orderNo));
    }
}
