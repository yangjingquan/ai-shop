package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.groupbuy.service.GroupBuyService;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.PaymentLog;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.service.OrderPaymentService;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final ProductService productService;
    private final GroupBuyService groupBuyService;
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaidCallback(String orderNo, String transactionId, String rawPayload) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED);
        }
        // MySQL row lock and payment_log unique keys provide durable idempotency. A short Redis lock can expire
        // during processing and is therefore not a correctness boundary for a money flow.
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
            return;
        }
        PaymentLog log = new PaymentLog();
        log.setOrderNo(orderNo);
        log.setTransactionId(transactionId);
        log.setAmount(order.getPayAmount());
        log.setRawPayload(rawPayload);
        try {
            paymentLogMapper.insert(log);
        } catch (DuplicateKeyException e) {
            PaymentLog existing = paymentLogMapper.selectOne(new LambdaQueryWrapper<PaymentLog>()
                    .eq(PaymentLog::getTransactionId, transactionId));
            if (existing != null && !orderNo.equals(existing.getOrderNo())) {
                throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "微信交易号已关联其他订单");
            }
            return;
        }

        order.setPayTime(LocalDateTime.now());
        order.setPayTransactionId(transactionId);
        order.setPayMethod(1);
        if (Integer.valueOf(1).equals(order.getOrderType())) {
            order.setStatus(OrderStatus.WAIT_GROUP.getCode());
            orderMapper.updateById(order);
            groupBuyService.handleOrderPaid(orderNo);
        } else {
            order.setStatus(OrderStatus.WAIT_SHIP.getCode());
            orderMapper.updateById(order);
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        Map<Long, Integer> salesByProduct = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId,
                        Collectors.summingInt(OrderItem::getQuantity)));
        for (Map.Entry<Long, Integer> e : salesByProduct.entrySet()) {
            orderMapper.addTotalSales(e.getKey(), e.getValue());
            productService.recalcProduct(e.getKey());
        }
    }
}
