package com.shop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.RefundCompletionService;
import com.shop.order.service.RefundReconciliationService;
import com.shop.order.service.WxPayService;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundReconciliationServiceImpl implements RefundReconciliationService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final RefundApplicationMapper refundMapper;
    private final OrderMapper orderMapper;
    private final WxPayService wxPayService;
    private final RefundCompletionService refundCompletionService;
    private final ObjectMapper objectMapper;

    @Override
    public int reconcilePending(int batchLimit) {
        if (batchLimit <= 0) {
            return 0;
        }
        List<RefundApplication> refunds = refundMapper.selectPendingReconciliation(batchLimit);
        int completed = 0;
        for (RefundApplication app : refunds) {
            String error = "";
            try {
                Order order = orderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, app.getOrderNo()));
                if (order == null) {
                    throw new IllegalStateException("退款关联订单不存在");
                }
                Refund wxRefund = app.getStatus() == RefundStatus.REFUNDING.getCode()
                        ? wxPayService.queryRefund(order, app.getOutRefundNo())
                        : wxPayService.createRefund(order, app.getOutRefundNo(), app.getReason(), app.getRefundAmount());
                validateResponse(app, order, wxRefund);
                applyResponse(app, order, wxRefund);
                if (app.getStatus() == RefundStatus.SUCCESS.getCode()) {
                    completed++;
                }
            } catch (Exception e) {
                error = abbreviate(e.getMessage());
                log.warn("退款主动对账失败, outRefundNo={}, error={}", app.getOutRefundNo(), error, e);
            } finally {
                refundMapper.markReconcileAttempt(app.getId(), LocalDateTime.now(), error);
            }
        }
        return completed;
    }

    private void validateResponse(RefundApplication app, Order order, Refund refund) {
        if (refund == null || refund.getStatus() == null) {
            throw new IllegalStateException("微信未返回退款状态");
        }
        if (!app.getOutRefundNo().equals(refund.getOutRefundNo())) {
            throw new IllegalStateException("微信退款单号不匹配");
        }
        if (refund.getOutTradeNo() != null && !refund.getOutTradeNo().isBlank()
                && !order.getOrderNo().equals(refund.getOutTradeNo())) {
            throw new IllegalStateException("微信退款关联订单不匹配");
        }
        if (refund.getAmount() == null || refund.getAmount().getRefund() == null
                || refund.getAmount().getRefund() != yuanToFen(app.getRefundAmount())) {
            throw new IllegalStateException("微信退款金额不匹配");
        }
    }

    private void applyResponse(RefundApplication app, Order order, Refund refund) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        app.setWxRefundId(refund.getRefundId() == null ? "" : refund.getRefundId());
        app.setRefundRawPayload(objectMapper.writeValueAsString(refund));
        if (refund.getStatus() == Status.SUCCESS) {
            app.setStatus(RefundStatus.SUCCESS.getCode());
            app.setRefundTime(parseTime(refund.getSuccessTime(), now));
            app.setRefundFailReason("");
            refundMapper.updateById(app);
            refundCompletionService.completeIfFullRefund(app, order, app.getRefundTime());
        } else if (refund.getStatus() == Status.PROCESSING) {
            app.setStatus(RefundStatus.REFUNDING.getCode());
            app.setRefundFailReason("");
            refundMapper.updateById(app);
        } else {
            app.setStatus(RefundStatus.FAILED.getCode());
            app.setRefundFailReason("微信退款状态：" + refund.getStatus().name());
            refundMapper.updateById(app);
        }
    }

    private long yuanToFen(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalStateException("退款金额为空");
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private LocalDateTime parseTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            return fallback;
        }
    }

    private String abbreviate(String message) {
        String value = message == null || message.isBlank() ? "未知错误" : message;
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
