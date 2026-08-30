package com.shop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderPaymentService;
import com.shop.order.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private WxPayService wxPayService;
    @Mock
    private OrderPaymentService orderPaymentService;

    private PaymentReconciliationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentReconciliationServiceImpl(
                orderMapper, wxPayService, orderPaymentService, new ObjectMapper());
    }

    @Test
    void routesSuccessfulQueryThroughIdempotentPaymentHandler() {
        Order order = waitingOrder();
        Transaction transaction = transaction(Transaction.TradeStateEnum.SUCCESS, 1234);
        when(orderMapper.selectPendingPaymentReconciliation(100)).thenReturn(List.of(order));
        when(wxPayService.queryOrder(order)).thenReturn(transaction);

        assertEquals(1, service.reconcilePending(100));

        verify(orderPaymentService).handlePaidCallback(eq(order.getOrderNo()), eq("wx-tx-1"), any(String.class));
        verify(orderMapper).markPaymentReconcileAttempt(eq(order.getId()), any(LocalDateTime.class), eq(""));
    }

    @Test
    void recordsAttemptWithoutChangingOrderWhenPaymentIsNotSuccessful() {
        Order order = waitingOrder();
        Transaction transaction = transaction(Transaction.TradeStateEnum.NOTPAY, 1234);
        when(orderMapper.selectPendingPaymentReconciliation(100)).thenReturn(List.of(order));
        when(wxPayService.queryOrder(order)).thenReturn(transaction);

        assertEquals(0, service.reconcilePending(100));

        verify(orderPaymentService, never()).handlePaidCallback(any(), any(), any());
        verify(orderMapper).markPaymentReconcileAttempt(eq(order.getId()), any(LocalDateTime.class), eq(""));
    }

    @Test
    void rejectsMismatchedAmountAndPersistsDiagnostic() {
        Order order = waitingOrder();
        Transaction transaction = transaction(Transaction.TradeStateEnum.SUCCESS, 999);
        when(orderMapper.selectPendingPaymentReconciliation(100)).thenReturn(List.of(order));
        when(wxPayService.queryOrder(order)).thenReturn(transaction);

        assertEquals(0, service.reconcilePending(100));

        verify(orderPaymentService, never()).handlePaidCallback(any(), any(), any());
        ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        verify(orderMapper).markPaymentReconcileAttempt(eq(order.getId()), any(LocalDateTime.class), error.capture());
        assertTrue(error.getValue().contains("金额不匹配"));
    }

    private Order waitingOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("26083012000000010001");
        order.setPayAmount(new BigDecimal("12.34"));
        return order;
    }

    private Transaction transaction(Transaction.TradeStateEnum state, int totalFen) {
        Transaction transaction = new Transaction();
        transaction.setTradeState(state);
        transaction.setOutTradeNo("26083012000000010001");
        transaction.setTransactionId("wx-tx-1");
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(totalFen);
        transaction.setAmount(amount);
        return transaction;
    }
}
