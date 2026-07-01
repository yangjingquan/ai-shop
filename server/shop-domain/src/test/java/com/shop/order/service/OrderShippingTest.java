package com.shop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
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
class OrderShippingTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RefundApplicationMapper refundApplicationMapper;

    private static final Long WX_USER = 3L;
    private static final Long MERCHANT = 1L;

    @Test
    void shipSuccess() {
        // 找一个 WAIT_SHIP 的订单（status=1）
        Order order = createWaitShipOrder();
        assertNotNull(order);

        orderService.ship(MERCHANT, order.getOrderNo(), "SF12345678");
        Order after = orderMapper.selectById(order.getId());
        assertEquals(OrderStatus.WAIT_RECEIVE.getCode(), after.getStatus());
        assertEquals("SF12345678", after.getShipNo());
        assertNotNull(after.getShipTime());
    }

    @Test
    void shipBadShipNoThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.ship(MERCHANT, "FAKE_NO_XXXX", "ab"));
        assertEquals(ErrorCode.SHIP_NO_INVALID.getCode(), ex.getCode());
    }

    @Test
    void confirmReceiveSuccess() {
        Order order = createWaitReceiveOrder();
        assertNotNull(order);

        orderService.confirmReceive(WX_USER, order.getOrderNo());
        Order after = orderMapper.selectById(order.getId());
        assertEquals(OrderStatus.FINISHED.getCode(), after.getStatus());
        assertNotNull(after.getFinishTime());
    }

    @Test
    void refundApplySuccess() {
        Order order = createWaitShipOrder();
        assertNotNull(order);

        orderService.refundApply(WX_USER, order.getOrderNo(), "不想要了");
        List<RefundApplication> apps = refundApplicationMapper.selectList(
                new LambdaQueryWrapper<RefundApplication>().eq(RefundApplication::getOrderNo, order.getOrderNo()));
        assertEquals(1, apps.size());
        assertEquals(RefundStatus.PENDING.getCode(), apps.get(0).getStatus());
    }

    @Test
    void refundDuplicateThrows() {
        Order order = createWaitShipOrder();
        assertNotNull(order);

        orderService.refundApply(WX_USER, order.getOrderNo(), "reason1");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.refundApply(WX_USER, order.getOrderNo(), "reason2"));
        assertEquals(ErrorCode.REFUND_ALREADY_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void refundApproveCancelsOrder() {
        Order order = createWaitShipOrder();
        assertNotNull(order);

        orderService.refundApply(WX_USER, order.getOrderNo(), "test");
        RefundApplication app = refundApplicationMapper.selectList(
                new LambdaQueryWrapper<RefundApplication>().eq(RefundApplication::getOrderNo, order.getOrderNo())).get(0);

        orderService.refundApprove(MERCHANT, app.getId(), true, null);
        Order after = orderMapper.selectById(order.getId());
        assertEquals(OrderStatus.CANCELLED.getCode(), after.getStatus());
        assertEquals("REFUNDED", after.getCancelReason());
    }

    // helpers

    private Order createWaitShipOrder() {
        // 在 test DB 中手工构造一个 WAIT_SHIP 订单（跳过 create 全流程避免外部依赖）
        Order order = new Order();
        order.setOrderNo("M4B_SHIP_" + System.nanoTime());
        order.setUserId(WX_USER);
        order.setMerchantId(MERCHANT);
        order.setStatus(OrderStatus.WAIT_SHIP.getCode());
        order.setTotalAmount(java.math.BigDecimal.valueOf(99));
        order.setPayAmount(java.math.BigDecimal.valueOf(99));
        order.setFreightAmount(java.math.BigDecimal.ZERO);
        order.setDiscountAmount(java.math.BigDecimal.ZERO);
        order.setAddressSnapshot("{}");
        orderMapper.insert(order);
        return order;
    }

    private Order createWaitReceiveOrder() {
        Order order = new Order();
        order.setOrderNo("M4B_RCV_" + System.nanoTime());
        order.setUserId(WX_USER);
        order.setMerchantId(MERCHANT);
        order.setStatus(OrderStatus.WAIT_RECEIVE.getCode());
        order.setTotalAmount(java.math.BigDecimal.valueOf(99));
        order.setPayAmount(java.math.BigDecimal.valueOf(99));
        order.setFreightAmount(java.math.BigDecimal.ZERO);
        order.setDiscountAmount(java.math.BigDecimal.ZERO);
        order.setAddressSnapshot("{}");
        order.setShipNo("SF12345678");
        orderMapper.insert(order);
        return order;
    }
}
