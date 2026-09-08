package com.shop.order.service;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderStateTransition;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.OrderStateTransitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Central state transition guard and immutable audit trail. */
@Service
@RequiredArgsConstructor
public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.WAIT_PAY, EnumSet.of(OrderStatus.WAIT_SHIP, OrderStatus.WAIT_GROUP,
                    OrderStatus.CANCELLED, OrderStatus.PRESALE_WAIT_BALANCE),
            OrderStatus.WAIT_GROUP, EnumSet.of(OrderStatus.GROUP_SUCCESS, OrderStatus.GROUP_FAILED_WAIT_REFUND),
            OrderStatus.GROUP_SUCCESS, EnumSet.of(OrderStatus.WAIT_SHIP),
            OrderStatus.GROUP_FAILED_WAIT_REFUND, EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.PRESALE_WAIT_BALANCE, EnumSet.of(OrderStatus.WAIT_SHIP, OrderStatus.PRESALE_OVERDUE,
                    OrderStatus.CANCELLED),
            OrderStatus.PRESALE_OVERDUE, EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.WAIT_SHIP, EnumSet.of(OrderStatus.WAIT_RECEIVE, OrderStatus.CANCELLED),
            OrderStatus.WAIT_RECEIVE, EnumSet.of(OrderStatus.FINISHED),
            OrderStatus.FINISHED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    private final OrderMapper orderMapper;
    private final OrderStateTransitionMapper transitionMapper;

    public void recordCreated(Order order, String event) {
        record(order, null, order.getStatus(), event, null);
    }

    public void transition(Order order, OrderStatus target, String event, String reason) {
        OrderStatus source = OrderStatus.fromCode(order.getStatus());
        if (source == target) return;
        if (!TRANSITIONS.getOrDefault(source, Set.of()).contains(target)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED.getCode(),
                    "订单状态不允许从" + source.getText() + "变更为" + target.getText());
        }
        order.setStatus(target.getCode());
        orderMapper.updateById(order);
        record(order, source.getCode(), target.getCode(), event, reason);
    }

    public void recordLegacyTransition(Order order, Integer from, Integer to, String event, String reason) {
        record(order, from, to, event, reason);
    }

    private void record(Order order, Integer from, Integer to, String event, String reason) {
        OrderStateTransition transition = new OrderStateTransition();
        transition.setOrderId(order.getId());
        transition.setOrderNo(order.getOrderNo());
        transition.setFromState(from);
        transition.setToState(to);
        transition.setEvent(event);
        transition.setReason(reason == null ? "" : reason);
        transition.setCreatedAt(LocalDateTime.now());
        transitionMapper.insert(transition);
    }
}
