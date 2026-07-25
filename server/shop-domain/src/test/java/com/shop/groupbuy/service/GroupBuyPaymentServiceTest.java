package com.shop.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.enums.GroupBuyGroupStatus;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class GroupBuyPaymentServiceTest {
    @Autowired private GroupBuyService groupBuyService;
    @Autowired private OrderPaymentService paymentService;
    @Autowired private OrderMapper orderMapper;
    @Autowired private GroupBuyGroupMapper groupMapper;

    private static final Long USER_A = 3L;
    private static final Long USER_B = 4L;
    private static final Long ADDR_ID = 12L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long SKU_ID = 9L;

    @Test
    void paidGroupOrderWaitsUntilGroupIsFull() {
        GroupBuyCreateVO opened = groupBuyService.openGroup(USER_A, req());
        paymentService.handlePaidCallback(opened.getOrderNo(), txn(), "{\"mock\":true}");

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, opened.getOrderNo()));
        GroupBuyGroup group = groupMapper.selectById(opened.getGroupId());

        assertEquals(OrderStatus.WAIT_GROUP.getCode(), order.getStatus());
        assertEquals(GroupBuyGroupStatus.WAIT_GROUP.getCode(), group.getStatus());
        assertEquals(1, group.getPaidCount());
    }

    @Test
    void paymentThatFillsGroupFormsAllPaidOrders() {
        GroupBuyCreateVO opened = groupBuyService.openGroup(USER_A, req());
        GroupBuyCreateVO joined = groupBuyService.joinGroup(USER_B, opened.getGroupId(), req());

        paymentService.handlePaidCallback(opened.getOrderNo(), txn(), "{\"mock\":true}");
        paymentService.handlePaidCallback(joined.getOrderNo(), txn(), "{\"mock\":true}");

        GroupBuyGroup group = groupMapper.selectById(opened.getGroupId());
        Order first = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, opened.getOrderNo()));
        Order second = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, joined.getOrderNo()));

        assertEquals(GroupBuyGroupStatus.FORMED.getCode(), group.getStatus());
        assertEquals(OrderStatus.GROUP_SUCCESS.getCode(), first.getStatus());
        assertEquals(OrderStatus.GROUP_SUCCESS.getCode(), second.getStatus());
    }

    private GroupBuyCreateRequest req() {
        GroupBuyCreateRequest req = new GroupBuyCreateRequest();
        req.setProductId(PRODUCT_ID);
        req.setSkuId(SKU_ID);
        req.setQuantity(1);
        req.setAddressId(ADDR_ID);
        return req;
    }

    private String txn() {
        return "MOCK_GB_" + UUID.randomUUID().toString().replace("-", "");
    }
}
