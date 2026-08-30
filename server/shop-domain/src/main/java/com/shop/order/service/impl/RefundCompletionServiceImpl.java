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
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.service.RefundCompletionService;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundCompletionServiceImpl implements RefundCompletionService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductService productService;
    private final GroupBuyMemberMapper memberMapper;
    private final GroupBuyGroupMapper groupMapper;

    @Override
    @Transactional
    public void completeIfFullRefund(RefundApplication refund, Order order, LocalDateTime completedAt) {
        if (refund.getRefundAmount() == null || order.getPayAmount() == null
                || refund.getRefundAmount().compareTo(order.getPayAmount()) < 0) {
            return;
        }

        if (order.getStatus() != OrderStatus.CANCELLED.getCode()) {
            if (order.getStatus() == OrderStatus.WAIT_SHIP.getCode()
                    || order.getStatus() == OrderStatus.GROUP_SUCCESS.getCode()) {
                releaseOrderStock(order);
            }

            order.setStatus(OrderStatus.CANCELLED.getCode());
            order.setCancelReason("REFUNDED");
            order.setCancelTime(completedAt);
            order.setUpdatedAt(completedAt);
            orderMapper.updateById(order);
        }

        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getOrderNo, order.getOrderNo()));
        if (member == null) {
            return;
        }
        if (member.getStatus() != GroupBuyMemberStatus.REFUNDED.getCode()) {
            member.setStatus(GroupBuyMemberStatus.REFUNDED.getCode());
            memberMapper.updateById(member);
        }

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

    private void releaseOrderStock(Order order) {
        List<com.shop.order.entity.OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<com.shop.order.entity.OrderItem>()
                        .eq(com.shop.order.entity.OrderItem::getOrderId, order.getId()));
        for (com.shop.order.entity.OrderItem item : items) {
            orderMapper.releaseStock(item.getSkuId(), item.getQuantity());
        }
        Set<Long> productIds = items.stream()
                .map(com.shop.order.entity.OrderItem::getProductId)
                .collect(Collectors.toSet());
        for (Long productId : productIds) {
            productService.recalcProduct(productId);
        }
    }
}
