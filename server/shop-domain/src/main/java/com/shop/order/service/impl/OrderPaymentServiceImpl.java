package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final ProductService productService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void handlePaidCallback(String orderNo, String transactionId, String rawPayload) {
        // 1. Redis 锁
        String lockKey = "order:pay:" + orderNo;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            return; // 已被另一个回调处理
        }
        try {
            // 2. SELECT FOR UPDATE
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo)
                            .last("FOR UPDATE"));
            if (order == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            // 3. 状态检查（幂等）
            if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
                return; // 已处理
            }
            // 4. 写 payment_log（UNIQUE 幂等）
            PaymentLog log = new PaymentLog();
            log.setOrderNo(orderNo);
            log.setTransactionId(transactionId);
            log.setAmount(order.getPayAmount());
            log.setRawPayload(rawPayload);
            try {
                paymentLogMapper.insert(log);
            } catch (DuplicateKeyException e) {
                return; // 已处理过
            }
            // 5. 更新订单状态
            order.setStatus(OrderStatus.WAIT_SHIP.getCode());
            order.setPayTime(LocalDateTime.now());
            order.setPayTransactionId(transactionId);
            order.setPayMethod(1);
            orderMapper.updateById(order);

            // 6. 累加 total_sales + recalcProduct
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            Map<Long, Integer> salesByProduct = items.stream()
                    .collect(Collectors.groupingBy(OrderItem::getProductId,
                            Collectors.summingInt(OrderItem::getQuantity)));
            for (Map.Entry<Long, Integer> e : salesByProduct.entrySet()) {
                orderMapper.addTotalSales(e.getKey(), e.getValue());
                productService.recalcProduct(e.getKey());
            }
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }
}
