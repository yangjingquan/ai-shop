package com.shop.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyGroupStatus;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderPaymentService;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    @Autowired private GroupBuyMemberMapper memberMapper;
    @Autowired private UserAddressMapper addressMapper;

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
        GroupBuyCreateVO joined = groupBuyService.joinGroup(USER_B, opened.getGroupId(), req(createAddress(USER_B)));

        paymentService.handlePaidCallback(opened.getOrderNo(), txn(), "{\"mock\":true}");
        paymentService.handlePaidCallback(joined.getOrderNo(), txn(), "{\"mock\":true}");

        GroupBuyGroup group = groupMapper.selectById(opened.getGroupId());
        Order first = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, opened.getOrderNo()));
        Order second = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, joined.getOrderNo()));

        assertEquals(GroupBuyGroupStatus.FORMED.getCode(), group.getStatus());
        assertEquals(OrderStatus.GROUP_SUCCESS.getCode(), first.getStatus());
        assertEquals(OrderStatus.GROUP_SUCCESS.getCode(), second.getStatus());
    }

    @Test
    void latePaymentAfterGroupFormedMarksExtraPaidOrderGroupSuccess() {
        GroupBuyCreateVO opened = groupBuyService.openGroup(USER_A, req());
        GroupBuyCreateVO joined = groupBuyService.joinGroup(USER_B, opened.getGroupId(), req(createAddress(USER_B)));
        GroupBuyCreateVO extra = groupBuyService.joinGroup(5L, opened.getGroupId(), req(createAddress(5L)));

        paymentService.handlePaidCallback(opened.getOrderNo(), txn(), "{\"mock\":true}");
        paymentService.handlePaidCallback(joined.getOrderNo(), txn(), "{\"mock\":true}");
        paymentService.handlePaidCallback(extra.getOrderNo(), txn(), "{\"mock\":true}");

        Order extraOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, extra.getOrderNo()));
        GroupBuyMember extraMember = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getOrderNo, extra.getOrderNo()));

        assertEquals(OrderStatus.GROUP_SUCCESS.getCode(), extraOrder.getStatus());
        assertEquals(GroupBuyMemberStatus.PAID.getCode(), extraMember.getStatus());
        assertNotNull(extraMember.getPaidAt());
    }

    @Test
    void paymentAfterGroupExpiredMarksOrderWaitRefundWithoutFormingGroup() {
        GroupBuyCreateVO opened = groupBuyService.openGroup(USER_A, req());
        GroupBuyGroup group = groupMapper.selectById(opened.getGroupId());
        group.setExpireAt(LocalDateTime.now().minusMinutes(1));
        groupMapper.updateById(group);

        paymentService.handlePaidCallback(opened.getOrderNo(), txn(), "{\"mock\":true}");

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, opened.getOrderNo()));
        GroupBuyGroup afterGroup = groupMapper.selectById(opened.getGroupId());
        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getOrderNo, opened.getOrderNo()));

        assertEquals(OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode(), order.getStatus());
        assertEquals(GroupBuyGroupStatus.FAILED_WAIT_REFUND.getCode(), afterGroup.getStatus());
        assertEquals(GroupBuyMemberStatus.WAIT_REFUND.getCode(), member.getStatus());
    }

    private GroupBuyCreateRequest req() {
        return req(ADDR_ID);
    }

    private GroupBuyCreateRequest req(Long addressId) {
        GroupBuyCreateRequest req = new GroupBuyCreateRequest();
        req.setProductId(PRODUCT_ID);
        req.setSkuId(SKU_ID);
        req.setQuantity(1);
        req.setAddressId(addressId);
        return req;
    }

    private Long createAddress(Long userId) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiver("Test");
        address.setPhone("13800138000");
        address.setRegion("Test Region");
        address.setDetail("Test Detail");
        address.setIsDefault(0);
        addressMapper.insert(address);
        return address.getId();
    }

    private String txn() {
        return "MOCK_GB_" + UUID.randomUUID().toString().replace("-", "");
    }
}
