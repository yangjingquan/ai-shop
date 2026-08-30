package com.shop.groupbuy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.dto.GroupBuyGroupVO;
import com.shop.groupbuy.dto.GroupBuyMemberVO;
import com.shop.groupbuy.dto.GroupBuyProductDetailVO;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyGroupStatus;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.groupbuy.service.GroupBuyService;
import com.shop.order.dto.AddressSnapshot;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.WxPayService;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import com.shop.user.entity.User;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import com.shop.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupBuyServiceImpl implements GroupBuyService {
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final UserAddressMapper userAddressMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GroupBuyGroupMapper groupMapper;
    private final GroupBuyMemberMapper memberMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final UserMapper userMapper;
    private final WxPayService wxPayService;

    @Override
    public PageResult<ProductListVO> productPage(int page, int size, Long merchantId, Long categoryId, String keyword) {
        return productService.publicPage(page, size, merchantId, categoryId, keyword, null, 1);
    }

    @Override
    public GroupBuyProductDetailVO productDetail(Long productId, Long merchantId) {
        ProductDetailVO product = productService.publicGet(productId, merchantId);
        if (product.getIsGroupBuy() == null || product.getIsGroupBuy() != 1) {
            throw new BusinessException(ErrorCode.GROUP_BUY_PRODUCT_NOT_FOUND);
        }
        validateGroupBuyConfig(product.getGroupBuyPrice(), product.getGroupBuyRequiredCount());
        GroupBuyProductDetailVO vo = new GroupBuyProductDetailVO();
        vo.setProduct(product);
        List<GroupBuyGroup> active = groupMapper.selectList(new LambdaQueryWrapper<GroupBuyGroup>()
                .eq(GroupBuyGroup::getProductId, productId)
                .eq(merchantId != null, GroupBuyGroup::getMerchantId, merchantId)
                .eq(GroupBuyGroup::getStatus, GroupBuyGroupStatus.WAIT_GROUP.getCode())
                .gt(GroupBuyGroup::getExpireAt, LocalDateTime.now())
                .orderByAsc(GroupBuyGroup::getExpireAt)
                .last("LIMIT 20"));
        vo.setGroups(active.stream().map(this::toGroupVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional
    public GroupBuyCreateVO openGroup(Long userId, GroupBuyCreateRequest req) {
        return createGroupOrder(userId, null, null, req, true);
    }

    @Override
    @Transactional
    public GroupBuyCreateVO openGroup(Long userId, Long merchantId, GroupBuyCreateRequest req) {
        return createGroupOrder(userId, merchantId, null, req, true);
    }

    @Override
    @Transactional
    public GroupBuyCreateVO joinGroup(Long userId, Long groupId, GroupBuyCreateRequest req) {
        return createGroupOrder(userId, null, groupId, req, false);
    }

    @Override
    @Transactional
    public GroupBuyCreateVO joinGroup(Long userId, Long merchantId, Long groupId, GroupBuyCreateRequest req) {
        return createGroupOrder(userId, merchantId, groupId, req, false);
    }

    @Override
    public GroupBuyGroupVO groupDetail(Long groupId) {
        return groupDetail(groupId, null);
    }

    @Override
    public GroupBuyGroupVO groupDetail(Long groupId, Long merchantId) {
        GroupBuyGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
        }
        if (merchantId != null && !merchantId.equals(group.getMerchantId())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
        }
        GroupBuyGroupVO vo = toGroupVO(group);
        User leader = userMapper.selectById(group.getLeaderUserId());
        if (leader != null) {
            vo.setLeaderNickname(leader.getNickname());
            vo.setLeaderAvatar(leader.getAvatar());
        }

        List<GroupBuyMember> members = memberMapper.selectList(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, groupId)
                .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.PAID.getCode())
                .orderByAsc(GroupBuyMember::getCreatedAt));
        List<Long> userIds = members.stream().map(GroupBuyMember::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> users = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(user -> users.put(user.getId(), user));
        }
        for (GroupBuyMember member : members) {
            GroupBuyMemberVO memberVO = new GroupBuyMemberVO();
            User user = users.get(member.getUserId());
            memberVO.setNickname(user == null || user.getNickname() == null ? "拼团成员" : user.getNickname());
            memberVO.setAvatar(user == null ? "" : user.getAvatar());
            memberVO.setLeader(group.getLeaderUserId().equals(member.getUserId()));
            memberVO.setStatus(member.getStatus());
            memberVO.setStatusText(GroupBuyMemberStatus.PAID.getText());
            vo.getMembers().add(memberVO);
        }
        return vo;
    }

    @Override
    @Transactional
    public void handleOrderPaid(String orderNo) {
        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getOrderNo, orderNo));
        if (member == null || member.getStatus() == GroupBuyMemberStatus.PAID.getCode()) {
            return;
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null || !Integer.valueOf(1).equals(order.getOrderType())
                || order.getStatus() != OrderStatus.WAIT_GROUP.getCode()
                || order.getPayTime() == null
                || order.getPayTransactionId() == null) {
            return;
        }
        GroupBuyGroup group = groupMapper.selectByIdForUpdate(member.getGroupId());
        if (group == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (group.getStatus() == GroupBuyGroupStatus.FORMED.getCode()) {
            markMemberPaid(member, now);
            order.setStatus(OrderStatus.GROUP_SUCCESS.getCode());
            orderMapper.updateById(order);
            refreshPaidCount(group);
            return;
        }
        if (group.getStatus() != GroupBuyGroupStatus.WAIT_GROUP.getCode()) {
            return;
        }
        if (group.getExpireAt() != null && !group.getExpireAt().isAfter(now)) {
            markMemberPaid(member, now);
            failGroupForTimeout(group);
            return;
        }

        markMemberPaid(member, now);
        Long paid = memberMapper.selectCount(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, group.getId())
                .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.PAID.getCode()));
        group.setPaidCount(paid.intValue());

        if (group.getPaidCount() >= group.getRequiredCount()) {
            group.setStatus(GroupBuyGroupStatus.FORMED.getCode());
            group.setFormedAt(now);
            groupMapper.updateById(group);

            List<GroupBuyMember> paidMembers = memberMapper.selectList(new LambdaQueryWrapper<GroupBuyMember>()
                    .eq(GroupBuyMember::getGroupId, group.getId())
                    .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.PAID.getCode()));
            for (GroupBuyMember paidMember : paidMembers) {
                Order paidOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, paidMember.getOrderNo())
                        .eq(Order::getStatus, OrderStatus.WAIT_GROUP.getCode()));
                if (paidOrder != null) {
                    paidOrder.setStatus(OrderStatus.GROUP_SUCCESS.getCode());
                    orderMapper.updateById(paidOrder);
                }
            }
        } else {
            groupMapper.updateById(group);
        }
    }

    private void markMemberPaid(GroupBuyMember member, LocalDateTime paidAt) {
        member.setStatus(GroupBuyMemberStatus.PAID.getCode());
        member.setPaidAt(paidAt);
        memberMapper.updateById(member);
    }

    private void refreshPaidCount(GroupBuyGroup group) {
        Long paid = memberMapper.selectCount(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, group.getId())
                .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.PAID.getCode()));
        group.setPaidCount(paid.intValue());
        groupMapper.updateById(group);
    }

    @Override
    @Transactional
    public int failExpiredGroups(int batchLimit) {
        int limit = Math.min(batchLimit, 1000);
        if (limit <= 0) {
            return 0;
        }

        List<GroupBuyGroup> expired = groupMapper.selectList(new LambdaQueryWrapper<GroupBuyGroup>()
                .eq(GroupBuyGroup::getStatus, GroupBuyGroupStatus.WAIT_GROUP.getCode())
                .lt(GroupBuyGroup::getExpireAt, LocalDateTime.now())
                .orderByAsc(GroupBuyGroup::getId)
                .last("LIMIT " + limit));
        int count = 0;
        for (GroupBuyGroup candidate : expired) {
            GroupBuyGroup group = groupMapper.selectByIdForUpdate(candidate.getId());
            if (group == null || group.getStatus() != GroupBuyGroupStatus.WAIT_GROUP.getCode()) {
                continue;
            }
            if (group.getPaidCount() >= group.getRequiredCount()) {
                continue;
            }
            failGroupForTimeout(group);
            count++;
        }
        return count;
    }

    private void failGroupForTimeout(GroupBuyGroup group) {
        group.setStatus(GroupBuyGroupStatus.FAILED_WAIT_REFUND.getCode());
        groupMapper.updateById(group);

        List<GroupBuyMember> members = memberMapper.selectList(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, group.getId()));
        for (GroupBuyMember member : members) {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, member.getOrderNo()));
            if (order == null) {
                continue;
            }
            if (member.getStatus() == GroupBuyMemberStatus.PAID.getCode()) {
                markPaidOrderWaitRefund(member, order);
            } else if (member.getStatus() == GroupBuyMemberStatus.WAIT_PAY.getCode()) {
                cancelUnpaidOrder(member, order);
            }
        }
    }

    private void markPaidOrderWaitRefund(GroupBuyMember member, Order order) {
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_GROUP.getCode())
                .set(Order::getStatus, OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode()));
        if (affected > 0 || order.getStatus() == OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode()) {
            member.setStatus(GroupBuyMemberStatus.WAIT_REFUND.getCode());
            memberMapper.updateById(member);
            createRefundApplication(order);
            if (affected > 0) {
                releaseOrderStock(order);
            }
        }
    }

    private void createRefundApplication(Order order) {
        Long count = refundApplicationMapper.selectCount(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, order.getOrderNo()));
        if (count != null && count > 0) {
            return;
        }
        RefundApplication app = new RefundApplication();
        app.setOrderNo(order.getOrderNo());
        app.setOutRefundNo("RF_" + order.getOrderNo() + "_"
                + UUID.randomUUID().toString().replace("-", ""));
        app.setUserId(order.getUserId());
        app.setMerchantId(order.getMerchantId());
        app.setReason("拼团未成团");
        app.setRefundAmount(order.getPayAmount());
        app.setStatus(RefundStatus.PENDING.getCode());
        refundApplicationMapper.insert(app);
    }

    private void cancelUnpaidOrder(GroupBuyMember member, Order order) {
        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
                .set(Order::getCancelReason, "GROUP_TIMEOUT")
                .set(Order::getCancelTime, now));
        if (affected > 0) {
            member.setStatus(GroupBuyMemberStatus.CANCELLED.getCode());
            memberMapper.updateById(member);
            releaseOrderStock(order);
            return;
        }

        Order latest = orderMapper.selectById(order.getId());
        if (latest != null && latest.getStatus() == OrderStatus.WAIT_GROUP.getCode()) {
            markPaidOrderWaitRefund(member, latest);
        }
    }

    private void releaseOrderStock(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            orderMapper.releaseStock(item.getSkuId(), item.getQuantity());
        }
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        for (Long productId : productIds) {
            productService.recalcProduct(productId);
        }
    }

    private GroupBuyCreateVO createGroupOrder(Long userId, Long merchantId, Long groupId,
                                              GroupBuyCreateRequest req, boolean openNewGroup) {
        Product product = productMapper.selectById(req.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1
                || product.getIsGroupBuy() == null || product.getIsGroupBuy() != 1) {
            throw new BusinessException(ErrorCode.GROUP_BUY_PRODUCT_NOT_FOUND);
        }
        if (merchantId != null && !merchantId.equals(product.getMerchantId())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_PRODUCT_NOT_FOUND);
        }
        validateGroupBuyConfig(product.getGroupBuyPrice(), product.getGroupBuyRequiredCount());
        ProductSku sku = skuMapper.selectById(req.getSkuId());
        if (sku == null || !Integer.valueOf(1).equals(sku.getActive())
                || !product.getId().equals(sku.getProductId()) || sku.getStock() < req.getQuantity()) {
            throw new BusinessException(ErrorCode.CART_ITEM_INVALID);
        }
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, req.getAddressId())
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        GroupBuyGroup group;
        if (openNewGroup) {
            group = new GroupBuyGroup();
            group.setMerchantId(product.getMerchantId());
            group.setProductId(product.getId());
            group.setLeaderUserId(userId);
            group.setRequiredCount(product.getGroupBuyRequiredCount());
            group.setPaidCount(0);
            group.setStatus(GroupBuyGroupStatus.WAIT_GROUP.getCode());
            group.setExpireAt(LocalDateTime.now().plusHours(24));
            groupMapper.insert(group);
        } else {
            group = groupMapper.selectByIdForUpdate(groupId);
            if (group == null || !group.getProductId().equals(product.getId())
                    || (merchantId != null && !merchantId.equals(group.getMerchantId()))) {
                throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
            }
            if (group.getStatus() != GroupBuyGroupStatus.WAIT_GROUP.getCode()) {
                throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_STATUS_INVALID);
            }
            if (group.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_EXPIRED);
            }
            if (group.getPaidCount() >= group.getRequiredCount()) {
                throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_FULL);
            }
            GroupBuyMember existing = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                    .eq(GroupBuyMember::getGroupId, group.getId())
                    .eq(GroupBuyMember::getUserId, userId));
            if (existing != null) {
                throw new BusinessException(ErrorCode.GROUP_BUY_ORDER_INVALID.getCode(), "您已参加该拼团，请勿重复下单");
            }
        }

        int affected = skuMapper.deductStock(sku.getId(), req.getQuantity());
        if (affected == 0) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        String orderNo = generateOrderNo(userId);
        BigDecimal total = product.getGroupBuyPrice().multiply(BigDecimal.valueOf(req.getQuantity()));

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(product.getMerchantId());
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        order.setOrderType(1);
        order.setGroupBuyGroupId(group.getId());
        order.setTotalAmount(total);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(total);
        order.setAddressSnapshot(toJson(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail())));
        order.setRemark(req.getRemark() != null ? req.getRemark() : "");
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setOrderNo(orderNo);
        item.setProductId(product.getId());
        item.setSkuId(sku.getId());
        item.setProductName(product.getName());
        item.setMainImage(product.getMainImage());
        item.setSpecText(sku.getSpecText());
        item.setUnitPrice(product.getGroupBuyPrice());
        item.setQuantity(req.getQuantity());
        item.setSubtotal(total);
        orderItemMapper.insert(item);

        GroupBuyMember member = new GroupBuyMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setOrderNo(orderNo);
        member.setStatus(GroupBuyMemberStatus.WAIT_PAY.getCode());
        memberMapper.insert(member);

        productService.recalcProduct(product.getId());

        GroupBuyCreateVO vo = new GroupBuyCreateVO();
        vo.setGroupId(group.getId());
        vo.setOrderNo(orderNo);
        vo.setPayAmount(total);
        if (merchantId != null) {
            vo.setPayParams(wxPayService.createJsapiPayParams(order));
        }
        return vo;
    }

    private void validateGroupBuyConfig(BigDecimal groupBuyPrice, Integer requiredCount) {
        if (groupBuyPrice == null || groupBuyPrice.compareTo(BigDecimal.ZERO) <= 0
                || requiredCount == null || requiredCount < 2) {
            throw new BusinessException(ErrorCode.GROUP_BUY_PRODUCT_CONFIG_INVALID);
        }
    }

    private GroupBuyGroupVO toGroupVO(GroupBuyGroup group) {
        GroupBuyGroupVO vo = new GroupBuyGroupVO();
        vo.setId(group.getId());
        vo.setProductId(group.getProductId());
        vo.setRequiredCount(group.getRequiredCount());
        vo.setPaidCount(group.getPaidCount());
        vo.setStatus(group.getStatus());
        vo.setStatusText(statusText(group.getStatus()));
        vo.setRemainingCount(Math.max(0, group.getRequiredCount() - group.getPaidCount()));
        if (group.getExpireAt() != null) {
            vo.setExpireAt(group.getExpireAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (group.getFormedAt() != null) {
            vo.setFormedAt(group.getFormedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return vo;
    }

    private String statusText(Integer status) {
        for (GroupBuyGroupStatus s : GroupBuyGroupStatus.values()) {
            if (s.getCode() == status) {
                return s.getText();
            }
        }
        return "";
    }

    private String generateOrderNo(Long userId) {
        Random rnd = new Random();
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String uidPart = String.format("%04d", userId % 10000);
        for (int i = 0; i < 100; i++) {
            String suffix = String.format("%04d", rnd.nextInt(10000));
            String no = prefix + uidPart + suffix;
            if (orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no)) == 0) {
                return no;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

}
