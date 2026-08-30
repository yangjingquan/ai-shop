package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyGroupStatus;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.RefundCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundCompletionServiceImpl implements RefundCompletionService {

    private final OrderMapper orderMapper;
    private final GroupBuyMemberMapper memberMapper;
    private final GroupBuyGroupMapper groupMapper;

    @Override
    @Transactional
    public void completeIfFullRefund(RefundApplication refund, Order order, LocalDateTime completedAt) {
        if (refund.getRefundAmount() == null || order.getPayAmount() == null
                || refund.getRefundAmount().compareTo(order.getPayAmount()) < 0) {
            return;
        }
        if (order.getStatus() != OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode()) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason("REFUNDED");
        order.setCancelTime(completedAt);
        order.setUpdatedAt(completedAt);
        orderMapper.updateById(order);

        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getOrderNo, order.getOrderNo()));
        if (member == null) {
            return;
        }
        member.setStatus(GroupBuyMemberStatus.REFUNDED.getCode());
        memberMapper.updateById(member);

        Long waitingCount = memberMapper.selectCount(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, member.getGroupId())
                .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.WAIT_REFUND.getCode()));
        if (waitingCount != null && waitingCount == 0) {
            GroupBuyGroup group = groupMapper.selectById(member.getGroupId());
            if (group != null && group.getStatus() == GroupBuyGroupStatus.FAILED_WAIT_REFUND.getCode()) {
                group.setStatus(GroupBuyGroupStatus.FAILED_REFUNDED.getCode());
                groupMapper.updateById(group);
            }
        }
    }
}
