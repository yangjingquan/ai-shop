package com.shop.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.dto.AdminRefundVO;
import com.shop.order.dto.AdminPaymentVO;
import com.shop.order.dto.OrderDetailVO;
import com.shop.order.dto.OrderListVO;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.OrderService;
import com.shop.order.service.PaymentReconciliationService;
import com.shop.order.service.RefundReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final MerchantMapper merchantMapper;
    private final OrderService orderService;
    private final PaymentLogMapper paymentLogMapper;
    private final PaymentReconciliationService paymentReconciliationService;
    private final RefundReconciliationService refundReconciliationService;

    @GetMapping("/payments/page")
    public ApiResult<PageResult<AdminPaymentVO>> payments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo) {
        IPage<AdminPaymentVO> result = paymentLogMapper.selectAdminPage(new Page<>(page, size), merchantId,
                normalize(orderNo), normalize(transactionId), createdFrom, createdTo);
        return ApiResult.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping("/payments/reconcile")
    public ApiResult<Map<String, Integer>> reconcilePayments() {
        return ApiResult.success(Map.of("paidCount", paymentReconciliationService.reconcilePending(100)));
    }

    @PostMapping("/refunds/reconcile")
    public ApiResult<Map<String, Integer>> reconcileRefunds() {
        return ApiResult.success(Map.of("successCount", refundReconciliationService.reconcilePending(100)));
    }

    @GetMapping("/orders/page")
    public ApiResult<PageResult<OrderListVO>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo) {
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getId);
        if (status != null) q.eq(Order::getStatus, status);
        if (merchantId != null) q.eq(Order::getMerchantId, merchantId);
        if (StringUtils.hasText(orderNo)) q.like(Order::getOrderNo, orderNo.trim());
        if (createdFrom != null) q.ge(Order::getCreatedAt, createdFrom);
        if (createdTo != null) q.lt(Order::getCreatedAt, createdTo);

        IPage<Order> result = orderMapper.selectPage(new Page<>(page, size), q);
        List<Long> merchantIds = result.getRecords().stream()
                .map(Order::getMerchantId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantMapper.selectBatchIds(merchantIds).forEach(m -> names.put(m.getId(), m.getName()));
        }
        List<OrderListVO> list = result.getRecords().stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setStatusText(OrderStatus.statusText(o.getStatus()));
            vo.setOrderType(o.getOrderType());
            vo.setPayAmount(o.getPayAmount());
            vo.setMerchantId(o.getMerchantId());
            vo.setMerchantName(names.getOrDefault(o.getMerchantId(), ""));
            vo.setCreatedAt(o.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        return ApiResult.success(PageResult.of(list, result.getTotal(), page, size));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResult<OrderDetailVO> orderDetail(@PathVariable String orderNo) {
        return ApiResult.success(orderService.adminDetail(orderNo));
    }

    @GetMapping("/refunds/page")
    public ApiResult<PageResult<AdminRefundVO>> refunds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long merchantId) {
        LambdaQueryWrapper<RefundApplication> q = new LambdaQueryWrapper<RefundApplication>()
                .orderByDesc(RefundApplication::getId);
        if (status != null) q.eq(RefundApplication::getStatus, status);
        if (merchantId != null) q.eq(RefundApplication::getMerchantId, merchantId);
        IPage<RefundApplication> result = refundApplicationMapper.selectPage(new Page<>(page, size), q);
        List<Long> merchantIds = result.getRecords().stream()
                .map(RefundApplication::getMerchantId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantMapper.selectBatchIds(merchantIds).forEach(m -> names.put(m.getId(), m.getName()));
        }
        List<AdminRefundVO> list = result.getRecords().stream().map(r -> {
            AdminRefundVO vo = new AdminRefundVO();
            vo.setId(r.getId());
            vo.setOrderNo(r.getOrderNo());
            vo.setOutRefundNo(r.getOutRefundNo());
            vo.setWxRefundId(r.getWxRefundId());
            vo.setUserId(r.getUserId());
            vo.setMerchantId(r.getMerchantId());
            vo.setMerchantName(names.getOrDefault(r.getMerchantId(), ""));
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
        }).collect(Collectors.toList());
        return ApiResult.success(PageResult.of(list, result.getTotal(), page, size));
    }

    private String refundStatusText(Integer status) {
        if (status == null) return "未知";
        for (RefundStatus item : RefundStatus.values()) {
            if (item.getCode() == status) return item.getText();
        }
        return "未知";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
