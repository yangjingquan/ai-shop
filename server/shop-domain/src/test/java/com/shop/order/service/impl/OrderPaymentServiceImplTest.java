package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.groupbuy.service.GroupBuyService;
import com.shop.order.entity.Order;
import com.shop.order.entity.PaymentLog;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private PaymentLogMapper paymentLogMapper;
    @Mock private RefundApplicationMapper refundApplicationMapper;
    @Mock private ProductService productService;
    @Mock private GroupBuyService groupBuyService;

    @Test
    void latePaymentAfterGroupTimeoutCreatesAutomaticRefund() {
        Order order = cancelledOrder("GROUP_TIMEOUT");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        OrderPaymentServiceImpl service = service();
        service.handlePaidCallback(order.getOrderNo(), "wx-late-1", "{\"mock\":true}");

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundApplicationMapper).insert(captor.capture());
        RefundApplication refund = captor.getValue();
        assertEquals(RefundStatus.PENDING.getCode(), refund.getStatus());
        assertEquals(1, refund.getAutoRefund());
        assertEquals(new BigDecimal("12.34"), refund.getRefundAmount());
        assertTrue(refund.getOutRefundNo().startsWith("RF_LATE_"));
        verify(orderMapper, never()).updateById(any(Order.class));
        verify(groupBuyService, never()).handleOrderPaid(any());
    }

    @Test
    void latePaymentAfterAdminCloseCreatesAutomaticRefund() {
        Order order = cancelledOrder("ADMIN_CANCEL");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        service().handlePaidCallback(order.getOrderNo(), "wx-admin-late-1", "{\"mock\":true}");

        ArgumentCaptor<RefundApplication> captor = ArgumentCaptor.forClass(RefundApplication.class);
        verify(refundApplicationMapper).insert(captor.capture());
        assertEquals(RefundStatus.PENDING.getCode(), captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getAutoRefund());
    }

    @Test
    void duplicateLatePaymentDoesNotCreateSecondRefund() {
        Order order = cancelledOrder("USER_CANCEL");
        PaymentLog existing = new PaymentLog();
        existing.setOrderNo(order.getOrderNo());
        existing.setTransactionId("wx-late-1");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(paymentLogMapper.insert(any(PaymentLog.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        when(paymentLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service().handlePaidCallback(order.getOrderNo(), "wx-late-1", "{\"mock\":true}");

        verify(refundApplicationMapper, never()).insert(any(RefundApplication.class));
    }

    @Test
    void successfulNormalPaymentMovesOrderToWaitingShip() {
        Order order = cancelledOrder("");
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        order.setCancelReason("");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());

        service().handlePaidCallback(order.getOrderNo(), "wx-normal-1", "{\"mock\":true}");

        assertEquals(OrderStatus.WAIT_SHIP.getCode(), order.getStatus());
        assertEquals("wx-normal-1", order.getPayTransactionId());
        verify(orderMapper).updateById(order);
        verify(refundApplicationMapper, never()).insert(any(RefundApplication.class));
    }

    private OrderPaymentServiceImpl service() {
        return new OrderPaymentServiceImpl(
                orderMapper, orderItemMapper, paymentLogMapper, refundApplicationMapper,
                productService, groupBuyService);
    }

    private Order cancelledOrder(String reason) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORDER_LATE_001");
        order.setUserId(3L);
        order.setMerchantId(1L);
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason(reason);
        order.setPayAmount(new BigDecimal("12.34"));
        return order;
    }
}
