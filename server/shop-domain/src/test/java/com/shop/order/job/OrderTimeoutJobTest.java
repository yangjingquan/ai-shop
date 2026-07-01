package com.shop.order.job;

import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.service.CartService;
import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.service.OrderService;
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
class OrderTimeoutJobTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    private static final Long WX_USER = 3L;
    private static final Long ADDR_ID = 12L;

    @Test
    void cancelExpiredShouldNotAffectFreshOrders() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        OrderCreateRequest creq = new OrderCreateRequest();
        creq.setCartItemIds(List.of(cid));
        creq.setAddressId(ADDR_ID);
        List<OrderCreateVO> vos = orderService.create(WX_USER, creq);

        // 刚创建的订单不应该被过期扫描取消（created_at < 30min ago）
        int count = orderService.cancelExpired(100);
        assertEquals(0, count); // 无过期订单
    }
}
