package com.shop.groupbuy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.dto.GroupBuyGroupVO;
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
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
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

    @Override
    public PageResult<ProductListVO> productPage(int page, int size, Long merchantId, Long categoryId, String keyword) {
        return productService.publicPage(page, size, merchantId, categoryId, keyword, null, 1);
    }

    @Override
    public GroupBuyProductDetailVO productDetail(Long productId, Long merchantId) {
        ProductDetailVO product = productService.publicGet(productId, merchantId);
        GroupBuyProductDetailVO vo = new GroupBuyProductDetailVO();
        vo.setProduct(product);
        List<GroupBuyGroup> active = groupMapper.selectList(new LambdaQueryWrapper<GroupBuyGroup>()
                .eq(GroupBuyGroup::getProductId, productId)
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
        return createGroupOrder(userId, null, req, true);
    }

    @Override
    @Transactional
    public GroupBuyCreateVO joinGroup(Long userId, Long groupId, GroupBuyCreateRequest req) {
        return createGroupOrder(userId, groupId, req, false);
    }

    @Override
    public GroupBuyGroupVO groupDetail(Long groupId) {
        GroupBuyGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
        }
        return toGroupVO(group);
    }

    @Override
    public void handleOrderPaid(String orderNo) {
    }

    @Override
    public int failExpiredGroups(int batchLimit) {
        return 0;
    }

    private GroupBuyCreateVO createGroupOrder(Long userId, Long groupId, GroupBuyCreateRequest req, boolean openNewGroup) {
        Product product = productMapper.selectById(req.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1
                || product.getIsGroupBuy() == null || product.getIsGroupBuy() != 1) {
            throw new BusinessException(ErrorCode.GROUP_BUY_PRODUCT_NOT_FOUND);
        }
        ProductSku sku = skuMapper.selectById(req.getSkuId());
        if (sku == null || !product.getId().equals(sku.getProductId()) || sku.getStock() < req.getQuantity()) {
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
            if (group == null || !group.getProductId().equals(product.getId())) {
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
        vo.setPayParams(mockPayParams(orderNo));
        return vo;
    }

    private GroupBuyGroupVO toGroupVO(GroupBuyGroup group) {
        GroupBuyGroupVO vo = new GroupBuyGroupVO();
        vo.setId(group.getId());
        vo.setProductId(group.getProductId());
        vo.setRequiredCount(group.getRequiredCount());
        vo.setPaidCount(group.getPaidCount());
        vo.setStatus(group.getStatus());
        vo.setStatusText(statusText(group.getStatus()));
        if (group.getExpireAt() != null) {
            vo.setExpireAt(group.getExpireAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
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

    private OrderCreateVO.PayParams mockPayParams(String orderNo) {
        OrderCreateVO.PayParams pp = new OrderCreateVO.PayParams();
        pp.setAppId("wx_mock");
        pp.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        pp.setNonceStr(UUID.randomUUID().toString().substring(0, 16));
        pp.setPackageStr("prepay_id=mock_" + orderNo);
        pp.setSignType("MD5");
        pp.setPaySign("MOCK_SIGN");
        return pp;
    }
}
