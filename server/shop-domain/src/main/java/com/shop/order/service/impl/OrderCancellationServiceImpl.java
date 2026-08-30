package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderCancellationService;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GroupBuyMemberMapper groupBuyMemberMapper;
    private final ProductService productService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelByUser(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId)
                .last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.canCancel(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        cancelLocked(order, "USER_CANCEL");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancelExpired(Long orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .last("FOR UPDATE"));
        if (order == null || order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
            return false;
        }
        cancelLocked(order, "TIMEOUT");
        return true;
    }

    private void cancelLocked(Order order, String reason) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            orderMapper.releaseStock(item.getSkuId(), item.getQuantity());
        }
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        for (Long productId : productIds) {
            productService.recalcProduct(productId);
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        markGroupBuyMemberCancelled(order);
    }

    private void markGroupBuyMemberCancelled(Order order) {
        if (!Integer.valueOf(1).equals(order.getOrderType())) {
            return;
        }
        GroupBuyMember member = groupBuyMemberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getOrderNo, order.getOrderNo())
                .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.WAIT_PAY.getCode()));
        if (member != null) {
            member.setStatus(GroupBuyMemberStatus.CANCELLED.getCode());
            groupBuyMemberMapper.updateById(member);
        }
    }
}
