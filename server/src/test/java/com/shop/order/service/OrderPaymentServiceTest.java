package com.shop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.service.CartService;
import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class OrderPaymentServiceTest {

    @Autowired
    private OrderPaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderMapper orderMapper;

    private static final Long WX_USER = 3L;
    private static final Long ADDR_ID = 12L;

    @Test
    void mockPayChangesStatus() {
        // 创建一个订单
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        OrderCreateRequest creq = new OrderCreateRequest();
        creq.setCartItemIds(List.of(cid));
        creq.setAddressId(ADDR_ID);
        List<OrderCreateVO> vos = orderService.create(WX_USER, creq);
        String orderNo = vos.get(0).getOrderNo();

        // mock-pay
        String txnId = "MOCK_TXN_" + UUID.randomUUID().toString().replace("-", "");
        paymentService.handlePaidCallback(orderNo, txnId, "{\"mock\":true}");

        // 验证状态变化
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertEquals(OrderStatus.WAIT_SHIP.getCode(), order.getStatus());
        assertNotNull(order.getPayTime());
    }

    @Test
    void doublePayShouldBeIdempotent() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        OrderCreateRequest creq = new OrderCreateRequest();
        creq.setCartItemIds(List.of(cid));
        creq.setAddressId(ADDR_ID);
        List<OrderCreateVO> vos = orderService.create(WX_USER, creq);
        String orderNo = vos.get(0).getOrderNo();

        String txnId = "MOCK_TXN_" + UUID.randomUUID().toString().replace("-", "");
        paymentService.handlePaidCallback(orderNo, txnId, "{\"mock\":true}");
        // 第二次不应抛异常
        assertDoesNotThrow(() ->
                paymentService.handlePaidCallback(orderNo, txnId, "{\"mock\":true}"));
    }

    @Test
    void payCancelledOrderShouldNoOp() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        OrderCreateRequest creq = new OrderCreateRequest();
        creq.setCartItemIds(List.of(cid));
        creq.setAddressId(ADDR_ID);
        List<OrderCreateVO> vos = orderService.create(WX_USER, creq);
        String orderNo = vos.get(0).getOrderNo();

        orderService.cancelByUser(WX_USER, orderNo);

        // 对已取消订单支付应不抛异常、状态不变
        String txnId = "MOCK_TXN_" + UUID.randomUUID().toString().replace("-", "");
        paymentService.handlePaidCallback(orderNo, txnId, "{\"mock\":true}");

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertEquals(OrderStatus.CANCELLED.getCode(), order.getStatus());
    }
}
