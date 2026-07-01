package com.shop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.service.CartService;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.order.dto.*;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderMapper orderMapper;

    private static final Long WX_USER = 3L;
    private static final Long ADDR_ID = 12L;

    @Test
    void previewShouldSucceed() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        cartService.add(WX_USER, req);

        List<Long> cids = cartService.list(WX_USER).stream().map(v -> v.getId()).toList();
        OrderPreviewRequest preq = new OrderPreviewRequest();
        preq.setCartItemIds(cids);
        preq.setAddressId(ADDR_ID);

        OrderPreviewVO vo = orderService.preview(WX_USER, preq);
        assertNotNull(vo);
        assertFalse(vo.getGroups().isEmpty());
    }

    @Test
    void createSingleOrderSuccess() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        OrderCreateRequest creq = new OrderCreateRequest();
        creq.setCartItemIds(List.of(cid));
        creq.setAddressId(ADDR_ID);

        List<OrderCreateVO> vos = orderService.create(WX_USER, creq);
        assertFalse(vos.isEmpty());
        String orderNo = vos.get(0).getOrderNo();

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNotNull(order);
        assertEquals(OrderStatus.WAIT_PAY.getCode(), order.getStatus());
    }

    @Test
    void cancelReturnsStock() {
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
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertEquals(OrderStatus.CANCELLED.getCode(), order.getStatus());
        assertEquals("USER_CANCEL", order.getCancelReason());
    }

    @Test
    void cancelNotOwnedThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelByUser(WX_USER, "NONEXIST_NO"));
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }
}
