package com.shop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderPaymentService;
import com.shop.order.service.PaymentReconciliationService;
import com.shop.order.service.WxPayService;
import com.shop.order.service.WechatPayOrderNotFoundException;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final OrderMapper orderMapper;
    private final WxPayService wxPayService;
    private final OrderPaymentService orderPaymentService;
    private final ObjectMapper objectMapper;

    @Override
    public int reconcilePending(int batchLimit) {
        if (batchLimit <= 0) {
            return 0;
        }
        List<Order> orders = orderMapper.selectPendingPaymentReconciliation(batchLimit);
        int paidCount = 0;
        for (Order order : orders) {
            String error = "";
            try {
                Transaction transaction = wxPayService.queryOrder(order);
                if (transaction != null && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                    validatePaidTransaction(order, transaction);
                    orderPaymentService.handlePaidCallback(order.getOrderNo(), transaction.getTransactionId(),
                            objectMapper.writeValueAsString(transaction));
                    paidCount++;
                }
            } catch (WechatPayOrderNotFoundException e) {
                // 微信侧没有支付单等价于本地订单仍未支付，不应把它记录成查单故障。
                log.info("微信支付订单不存在，按未支付订单处理, orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                error = abbreviate(e.getMessage());
                log.warn("主动查单失败, orderNo={}, error={}", order.getOrderNo(), error, e);
            } finally {
                orderMapper.markPaymentReconcileAttempt(order.getId(), LocalDateTime.now(), error);
            }
        }
        return paidCount;
    }

    private void validatePaidTransaction(Order order, Transaction transaction) {
        if (!order.getOrderNo().equals(transaction.getOutTradeNo())) {
            throw new IllegalStateException("微信支付订单号不匹配");
        }
        if (transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
            throw new IllegalStateException("微信支付交易号为空");
        }
        if (transaction.getAmount() == null || transaction.getAmount().getTotal() == null) {
            throw new IllegalStateException("微信支付金额为空");
        }
        int expectedFen = order.getPayAmount().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        if (transaction.getAmount().getTotal() != expectedFen) {
            throw new IllegalStateException("微信支付金额不匹配");
        }
    }

    private String abbreviate(String message) {
        String value = message == null || message.isBlank() ? "未知错误" : message;
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
