package com.shop.order.service.impl;

import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.coupon.service.CouponService;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private GroupBuyMemberMapper groupBuyMemberMapper;
    @Mock
    private ProductService productService;
    @Mock
    private CouponService couponService;

    @Test
    void cancelsExpiredOrderInItsOwnTransactionAndRestoresStock() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("26083000000000010001");
        order.setOrderType(0);
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        OrderItem item = new OrderItem();
        item.setSkuId(3L);
        item.setProductId(4L);
        item.setQuantity(2);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        OrderCancellationServiceImpl service = new OrderCancellationServiceImpl(
                orderMapper, orderItemMapper, groupBuyMemberMapper, productService, couponService);

        assertTrue(service.cancelExpired(1L));
        assertEquals(OrderStatus.CANCELLED.getCode(), order.getStatus());
        assertEquals("TIMEOUT", order.getCancelReason());
        verify(orderMapper).releaseStock(3L, 2);
        verify(productService).recalcProduct(4L);
        ArgumentCaptor<Order> updated = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(updated.capture());
        assertEquals(OrderStatus.CANCELLED.getCode(), updated.getValue().getStatus());
    }

    @Test
    void adminCanCloseUnpaidOrderAndRestoreStock() {
        Order order = new Order();
        order.setId(2L);
        order.setOrderNo("26083000000000010002");
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        OrderItem item = new OrderItem();
        item.setSkuId(5L);
        item.setProductId(6L);
        item.setQuantity(1);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        OrderCancellationServiceImpl service = new OrderCancellationServiceImpl(
                orderMapper, orderItemMapper, groupBuyMemberMapper, productService, couponService);

        service.cancelByAdmin(order.getOrderNo());

        assertEquals(OrderStatus.CANCELLED.getCode(), order.getStatus());
        assertEquals("ADMIN_CANCEL", order.getCancelReason());
        verify(orderMapper).releaseStock(5L, 1);
        verify(productService).recalcProduct(6L);
    }
}
