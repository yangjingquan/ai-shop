package com.shop.groupbuy.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyGroupStatus;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.groupbuy.service.GroupBuyService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class GroupBuyTimeoutJobTest {
    @Autowired private GroupBuyService groupBuyService;
    @Autowired private OrderPaymentService paymentService;
    @Autowired private GroupBuyGroupMapper groupMapper;
    @Autowired private GroupBuyMemberMapper memberMapper;
    @Autowired private OrderMapper orderMapper;

    private static final Long USER_A = 3L;
    private static final Long ADDR_ID = 12L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long SKU_ID = 9L;

    @Test
    void failExpiredGroupsMarksPaidOrdersWaitRefund() {
        GroupBuyCreateVO opened = groupBuyService.openGroup(USER_A, req());
        paymentService.handlePaidCallback(opened.getOrderNo(), txn(), "{\"mock\":true}");
        GroupBuyGroup group = groupMapper.selectById(opened.getGroupId());
        group.setExpireAt(LocalDateTime.now().minusMinutes(1));
        groupMapper.updateById(group);

        int count = groupBuyService.failExpiredGroups(10);

        assertEquals(1, count);
        GroupBuyGroup afterGroup = groupMapper.selectById(opened.getGroupId());
        Order afterOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, opened.getOrderNo()));
        GroupBuyMember afterMember = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getOrderNo, opened.getOrderNo()));

        assertEquals(GroupBuyGroupStatus.FAILED_WAIT_REFUND.getCode(), afterGroup.getStatus());
        assertEquals(OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode(), afterOrder.getStatus());
        assertEquals(GroupBuyMemberStatus.WAIT_REFUND.getCode(), afterMember.getStatus());
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
        return "MOCK_GB_TIMEOUT_" + UUID.randomUUID().toString().replace("-", "");
    }
}
