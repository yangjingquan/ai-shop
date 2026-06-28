package com.shop.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.order.dto.*;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.OrderService;
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

    @Data
    public static class ShipRequest {
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{5,30}$")
        private String shipNo;
    }

    @OpLog(action = "ORDER_SHIP", targetType = "ORDER")
    @PostMapping("/order/ship")
    public ApiResult<Void> ship(@RequestParam String orderNo, @RequestBody @Valid ShipRequest req) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        orderService.ship(merchantId, orderNo, req.getShipNo());
        return ApiResult.success(null);
    }

    @GetMapping("/order/page")
    public ApiResult<PageResult<OrderListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .orderByDesc(Order::getId);
        if (status != null) {
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

    @GetMapping("/refund/list")
    public ApiResult<List<RefundApplication>> refundList(
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
        return ApiResult.success(result.getRecords());
    }

    @Data
    public static class RefundApproveRequest {
        private boolean approved;
        private String rejectReason;
    }

    @OpLog(action = "REFUND_APPROVE", targetType = "REFUND")
    @PostMapping("/refund/{refundId}/approve")
    public ApiResult<Void> refundApprove(@PathVariable Long refundId,
                                         @RequestBody RefundApproveRequest req) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        orderService.refundApprove(merchantId, refundId, req.isApproved(), req.getRejectReason());
        return ApiResult.success(null);
    }
}
