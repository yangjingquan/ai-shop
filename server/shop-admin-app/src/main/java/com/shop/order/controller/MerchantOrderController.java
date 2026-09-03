package com.shop.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.aop.OpLog;
import com.shop.common.aop.RateLimit;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.order.dto.*;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.OrderService;
import com.shop.order.service.LogisticsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final LogisticsService logisticsService;

    @Data
    public static class ShipRequest {
        private String shipCompany;

        private String shipperCode;

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{5,30}$")
        private String shipNo;
    }

    @OpLog(action = "ORDER_SHIP", targetType = "ORDER", targetIdExpr = "#orderNo")
    @PostMapping("/order/ship")
    @RequirePermission("merchant:order:ship")
    public ApiResult<Void> ship(@RequestParam String orderNo, @RequestBody @Valid ShipRequest req) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        orderService.ship(merchantId, orderNo, req.getShipCompany(), req.getShipperCode(), req.getShipNo());
        return ApiResult.success(null);
    }

    @GetMapping("/order/{orderNo}/logistics")
    @RequirePermission("merchant:order:logistics:view")
    public ApiResult<LogisticsTrackingVO> logistics(@PathVariable String orderNo) {
        return ApiResult.success(logisticsService.trackForMerchant(
                CurrentUserHolder.get().getMerchantId(), orderNo, false));
    }

    @RateLimit(key = "logistics_refresh", limit = 1, windowSec = 60)
    @PostMapping("/order/{orderNo}/logistics/refresh")
    @RequirePermission("merchant:order:logistics:refresh")
    public ApiResult<LogisticsTrackingVO> refreshLogistics(@PathVariable String orderNo) {
        return ApiResult.success(logisticsService.trackForMerchant(
                CurrentUserHolder.get().getMerchantId(), orderNo, true));
    }

    @GetMapping("/order/page")
    @RequirePermission("merchant:order:view")
    public ApiResult<PageResult<OrderListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String scope) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .orderByDesc(Order::getId);
        if ("shipable".equals(scope)) {
            q.in(Order::getStatus, 1, 6);
        } else if (status != null) {
            q.eq(Order::getStatus, status);
        }
        IPage<Order> result = orderMapper.selectPage(new Page<>(page, size), q);
        List<OrderListVO> list = new ArrayList<>();
        for (Order o : result.getRecords()) {
            OrderListVO vo = new OrderListVO();
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setStatusText(OrderStatus.statusText(o.getStatus()));
            vo.setPayAmount(o.getPayAmount());
            vo.setCreatedAt(o.getCreatedAt());
            list.add(vo);
        }
        return ApiResult.success(PageResult.of(list, result.getTotal(), page, size));
    }

    @GetMapping("/order/{orderNo}")
    @RequirePermission("merchant:order:detail")
    public ApiResult<OrderDetailVO> detail(@PathVariable String orderNo) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        return ApiResult.success(orderService.merchantDetail(merchantId, orderNo));
    }

    @GetMapping("/refund/list")
    @RequirePermission("merchant:refund:view")
    public ApiResult<PageResult<AdminRefundVO>> refundList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        LambdaQueryWrapper<RefundApplication> q = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getMerchantId, merchantId)
                .orderByDesc(RefundApplication::getId);
        if (status != null) {
            q.eq(RefundApplication::getStatus, status);
        }
        IPage<RefundApplication> result = refundApplicationMapper.selectPage(new Page<>(page, size), q);
        List<AdminRefundVO> list = result.getRecords().stream().map(r -> {
            AdminRefundVO vo = new AdminRefundVO();
            vo.setId(r.getId());
            vo.setOrderNo(r.getOrderNo());
            vo.setOutRefundNo(r.getOutRefundNo());
            vo.setWxRefundId(r.getWxRefundId());
            vo.setUserId(r.getUserId());
            vo.setMerchantId(r.getMerchantId());
            vo.setReason(r.getReason());
            vo.setEvidenceUrls(r.getEvidenceUrls());
            vo.setStatus(r.getStatus());
            vo.setStatusText(refundStatusText(r.getStatus()));
            vo.setRejectReason(r.getRejectReason());
            vo.setRefundAmount(r.getRefundAmount());
            vo.setRefundFailReason(r.getRefundFailReason());
            vo.setRefundTime(r.getRefundTime());
            vo.setAutoRefund(r.getAutoRefund());
            vo.setReturnRequired(r.getReturnRequired());
            vo.setReturnShipCompany(r.getReturnShipCompany());
            vo.setReturnShipNo(r.getReturnShipNo());
            vo.setReturnShipTime(r.getReturnShipTime());
            vo.setReturnReceivedTime(r.getReturnReceivedTime());
            vo.setReturnReceiveNote(r.getReturnReceiveNote());
            vo.setRefundReconcileAt(r.getRefundReconcileAt());
            vo.setRefundReconcileAttempts(r.getRefundReconcileAttempts());
            vo.setRefundReconcileError(r.getRefundReconcileError());
            vo.setCreatedAt(r.getCreatedAt());
            vo.setUpdatedAt(r.getUpdatedAt());
            return vo;
        }).toList();
        return ApiResult.success(PageResult.of(list, result.getTotal(), page, size));
    }

    private String refundStatusText(Integer status) {
        for (RefundStatus value : RefundStatus.values()) {
            if (value.getCode() == (status == null ? -1 : status)) {
                return value.getText();
            }
        }
        return "未知状态";
    }

    @Data
    public static class RefundApproveRequest {
        private boolean approved;
        private String rejectReason;
    }

    @OpLog(action = "REFUND_APPROVE", targetType = "REFUND", targetIdExpr = "#refundId")
    @PostMapping("/refund/{refundId}/approve")
    @RequirePermission("merchant:refund:approve")
    public ApiResult<Void> refundApprove(@PathVariable Long refundId,
                                         @RequestBody RefundApproveRequest req) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        orderService.refundApprove(merchantId, refundId, req.isApproved(), req.getRejectReason());
        return ApiResult.success(null);
    }

    @OpLog(action = "REFUND_RETRY", targetType = "REFUND", targetIdExpr = "#refundId")
    @PostMapping("/refund/{refundId}/retry")
    @RequirePermission("merchant:refund:retry")
    public ApiResult<Void> retryRefund(@PathVariable Long refundId) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        orderService.refundApprove(merchantId, refundId, true, null);
        return ApiResult.success(null);
    }

    @Data
    public static class ReturnReceiveRequest {
        @jakarta.validation.constraints.Size(max = 255)
        private String note;
    }

    @OpLog(action = "REFUND_RETURN_RECEIVE", targetType = "REFUND", targetIdExpr = "#refundId")
    @PostMapping("/refund/{refundId}/return-received")
    @RequirePermission("merchant:refund:return-received")
    public ApiResult<Void> confirmReturnReceived(@PathVariable Long refundId,
                                                  @RequestBody(required = false) @Valid ReturnReceiveRequest req) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        orderService.confirmReturnReceived(merchantId, refundId, req == null ? "" : req.getNote());
        return ApiResult.success(null);
    }
}
