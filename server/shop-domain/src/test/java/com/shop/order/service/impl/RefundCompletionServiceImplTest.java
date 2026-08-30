package com.shop.order.service.impl;

import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCompletionServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductService productService;
    @Mock
    private GroupBuyMemberMapper memberMapper;
    @Mock
    private GroupBuyGroupMapper groupMapper;

    @Test
    void onlyClosesAndReplenishesAfterFullRefundSuccess() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("26083000000000010001");
        order.setStatus(OrderStatus.WAIT_SHIP.getCode());
        order.setPayAmount(new BigDecimal("12.34"));
        RefundApplication refund = new RefundApplication();
        refund.setRefundAmount(new BigDecimal("12.34"));
        OrderItem item = new OrderItem();
        item.setSkuId(3L);
        item.setProductId(4L);
        item.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(memberMapper.selectOne(any())).thenReturn(null);

        RefundCompletionServiceImpl service = new RefundCompletionServiceImpl(
                orderMapper, orderItemMapper, productService, memberMapper, groupMapper);
        service.completeIfFullRefund(refund, order, LocalDateTime.of(2026, 8, 30, 20, 0));

        assertEquals(OrderStatus.CANCELLED.getCode(), order.getStatus());
        assertEquals("REFUNDED", order.getCancelReason());
        verify(orderMapper).releaseStock(3L, 2);
        verify(productService).recalcProduct(4L);
        verify(orderMapper).updateById(order);
    }
}
