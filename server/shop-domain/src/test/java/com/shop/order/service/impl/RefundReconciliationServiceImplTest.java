package com.shop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.RefundCompletionService;
import com.shop.order.service.WxPayService;
import com.wechat.pay.java.service.refund.model.Amount;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
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
class RefundReconciliationServiceImplTest {

    @Mock
    private RefundApplicationMapper refundMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private WxPayService wxPayService;
    @Mock
    private RefundCompletionService completionService;

    private RefundReconciliationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefundReconciliationServiceImpl(
                refundMapper, orderMapper, wxPayService, completionService, new ObjectMapper());
    }

    @Test
    void startsAutomaticGroupRefundWithoutMerchantApproval() {
        RefundApplication app = refund(RefundStatus.PENDING);
        Order order = order();
        Refund response = response(Status.PROCESSING, 1234);
        when(refundMapper.selectPendingReconciliation(100)).thenReturn(List.of(app));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(wxPayService.createRefund(order, app.getOutRefundNo(), app.getReason(), app.getRefundAmount()))
                .thenReturn(response);

        assertEquals(0, service.reconcilePending(100));

        assertEquals(RefundStatus.REFUNDING.getCode(), app.getStatus());
        verify(wxPayService, never()).queryRefund(any(), any());
        verify(refundMapper).markReconcileAttempt(eq(app.getId()), any(LocalDateTime.class), eq(""));
    }

    @Test
    void completesRefundFromActiveQueryAndClosesGroupLifecycle() {
        RefundApplication app = refund(RefundStatus.REFUNDING);
        Order order = order();
        Refund response = response(Status.SUCCESS, 1234);
        response.setSuccessTime("2026-08-30T17:30:00+08:00");
        when(refundMapper.selectPendingReconciliation(100)).thenReturn(List.of(app));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(wxPayService.queryRefund(order, app.getOutRefundNo())).thenReturn(response);

        assertEquals(1, service.reconcilePending(100));

        assertEquals(RefundStatus.SUCCESS.getCode(), app.getStatus());
        verify(completionService).completeIfFullRefund(eq(app), eq(order), any(LocalDateTime.class));
    }

    @Test
    void preservesStateAndRecordsAmountMismatch() {
        RefundApplication app = refund(RefundStatus.REFUNDING);
        Order order = order();
        when(refundMapper.selectPendingReconciliation(100)).thenReturn(List.of(app));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(wxPayService.queryRefund(order, app.getOutRefundNo())).thenReturn(response(Status.SUCCESS, 999));

        assertEquals(0, service.reconcilePending(100));

        assertEquals(RefundStatus.REFUNDING.getCode(), app.getStatus());
        verify(completionService, never()).completeIfFullRefund(any(), any(), any());
        ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        verify(refundMapper).markReconcileAttempt(eq(app.getId()), any(LocalDateTime.class), error.capture());
        assertTrue(error.getValue().contains("金额不匹配"));
    }

    private RefundApplication refund(RefundStatus status) {
        RefundApplication app = new RefundApplication();
        app.setId(8L);
        app.setOrderNo("26083012000000010001");
        app.setOutRefundNo("RF_26083012000000010001_1");
        app.setReason("拼团未成团");
        app.setRefundAmount(new BigDecimal("12.34"));
        app.setStatus(status.getCode());
        app.setAutoRefund(1);
        return app;
    }

    private Order order() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("26083012000000010001");
        order.setPayAmount(new BigDecimal("12.34"));
        return order;
    }

    private Refund response(Status status, long refundFen) {
        Refund refund = new Refund();
        refund.setStatus(status);
        refund.setOutRefundNo("RF_26083012000000010001_1");
        refund.setOutTradeNo("26083012000000010001");
        refund.setRefundId("wx-refund-1");
        Amount amount = new Amount();
        amount.setRefund(refundFen);
        refund.setAmount(amount);
        return refund;
    }
}
