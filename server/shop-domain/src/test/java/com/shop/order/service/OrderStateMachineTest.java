package com.shop.order.service;

import com.shop.common.exception.BusinessException;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderStateTransition;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.OrderStateTransitionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderStateMachineTest {
    @Test
    void recordsCreationAndValidPaymentTransition() {
        OrderMapper orders = mock(OrderMapper.class);
        OrderStateTransitionMapper transitions = mock(OrderStateTransitionMapper.class);
        OrderStateMachine machine = new OrderStateMachine(orders, transitions);
        Order order = order(1L, "O100", OrderStatus.WAIT_PAY);

        machine.recordCreated(order, "ORDER_CREATED");
        machine.transition(order, OrderStatus.WAIT_SHIP, "PAYMENT_SUCCEEDED", "微信支付成功");

        assertEquals(OrderStatus.WAIT_SHIP.getCode(), order.getStatus());
        verify(orders).updateById(order);
        ArgumentCaptor<OrderStateTransition> audit = ArgumentCaptor.forClass(OrderStateTransition.class);
        verify(transitions, org.mockito.Mockito.times(2)).insert(audit.capture());
        assertEquals(null, audit.getAllValues().get(0).getFromState());
        assertEquals(OrderStatus.WAIT_PAY.getCode(), audit.getAllValues().get(0).getToState());
        assertEquals(OrderStatus.WAIT_PAY.getCode(), audit.getAllValues().get(1).getFromState());
        assertEquals(OrderStatus.WAIT_SHIP.getCode(), audit.getAllValues().get(1).getToState());
    }

    @Test
    void rejectsInvalidStateTransitionWithoutPersisting() {
        OrderMapper orders = mock(OrderMapper.class);
        OrderStateTransitionMapper transitions = mock(OrderStateTransitionMapper.class);
        OrderStateMachine machine = new OrderStateMachine(orders, transitions);
        Order order = order(1L, "O101", OrderStatus.WAIT_PAY);

        assertThrows(BusinessException.class,
                () -> machine.transition(order, OrderStatus.FINISHED, "ILLEGAL", "test"));
        assertEquals(OrderStatus.WAIT_PAY.getCode(), order.getStatus());
        org.mockito.Mockito.verifyNoInteractions(orders, transitions);
    }

    private Order order(Long id, String orderNo, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setStatus(status.getCode());
        return order;
    }
}
