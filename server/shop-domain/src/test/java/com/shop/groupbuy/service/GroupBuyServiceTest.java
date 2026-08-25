package com.shop.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
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
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class GroupBuyServiceTest {
    @Autowired private GroupBuyService groupBuyService;
    @Autowired private GroupBuyGroupMapper groupMapper;
    @Autowired private GroupBuyMemberMapper memberMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private ProductMapper productMapper;

    private static final Long WX_USER = 3L;
    private static final Long ADDR_ID = 12L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long SKU_ID = 9L;

    @Test
    void openGroupCreatesGroupMemberAndWaitPayOrder() {
        enableGroupBuySeedProduct();

        GroupBuyCreateRequest req = request();

        GroupBuyCreateVO vo = groupBuyService.openGroup(WX_USER, req);

        assertNotNull(vo.getOrderNo());
        assertNotNull(vo.getGroupId());
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, vo.getOrderNo()));
        assertEquals(OrderStatus.WAIT_PAY.getCode(), order.getStatus());
        assertEquals(1, order.getOrderType());
        assertEquals(vo.getGroupId(), order.getGroupBuyGroupId());

        GroupBuyGroup group = groupMapper.selectById(vo.getGroupId());
        assertEquals(GroupBuyGroupStatus.WAIT_GROUP.getCode(), group.getStatus());
        assertEquals(0, group.getPaidCount());
        assertNotNull(group.getExpireAt());

        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getOrderNo, vo.getOrderNo()));
        assertEquals(GroupBuyMemberStatus.WAIT_PAY.getCode(), member.getStatus());
    }

    @Test
    void joinGroupCreatesMemberAndWaitPayOrder() {
        enableGroupBuySeedProduct();
        GroupBuyCreateRequest req = request();
        GroupBuyCreateVO opened = groupBuyService.openGroup(WX_USER, req);

        GroupBuyCreateVO joined = groupBuyService.joinGroup(4L, opened.getGroupId(), req);

        assertNotNull(joined.getOrderNo());
        assertEquals(opened.getGroupId(), joined.getGroupId());
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, joined.getOrderNo()));
        assertEquals(OrderStatus.WAIT_PAY.getCode(), order.getStatus());
        assertEquals(1, order.getOrderType());
        assertEquals(opened.getGroupId(), order.getGroupBuyGroupId());
        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getOrderNo, joined.getOrderNo()));
        assertEquals(GroupBuyMemberStatus.WAIT_PAY.getCode(), member.getStatus());
    }

    @Test
    void joinGroupRejectsMismatchedProduct() {
        enableGroupBuySeedProduct();
        GroupBuyCreateVO opened = groupBuyService.openGroup(WX_USER, request());
        GroupBuyCreateRequest req = request();
        req.setProductId(2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupBuyService.joinGroup(4L, opened.getGroupId(), req));

        assertEquals(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void productDetailRejectsOrdinaryProduct() {
        Product product = productMapper.selectById(PRODUCT_ID);
        product.setIsGroupBuy(0);
        product.setGroupBuyPrice(null);
        product.setGroupBuyRequiredCount(null);
        productMapper.updateById(product);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupBuyService.productDetail(PRODUCT_ID, product.getMerchantId()));

        assertEquals(ErrorCode.GROUP_BUY_PRODUCT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void openGroupRejectsInvalidGroupBuyConfig() {
        Product product = productMapper.selectById(PRODUCT_ID);
        product.setIsGroupBuy(1);
        product.setGroupBuyPrice(null);
        product.setGroupBuyRequiredCount(null);
        productMapper.updateById(product);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupBuyService.openGroup(WX_USER, request()));

        assertEquals(ErrorCode.GROUP_BUY_PRODUCT_CONFIG_INVALID.getCode(), ex.getCode());
    }

    private GroupBuyCreateRequest request() {
        GroupBuyCreateRequest req = new GroupBuyCreateRequest();
        req.setProductId(PRODUCT_ID);
        req.setSkuId(SKU_ID);
        req.setQuantity(1);
        req.setAddressId(ADDR_ID);
        return req;
    }

    private void enableGroupBuySeedProduct() {
        Product product = productMapper.selectById(PRODUCT_ID);
        product.setIsGroupBuy(1);
        product.setGroupBuyPrice(new BigDecimal("88.00"));
        product.setGroupBuyRequiredCount(3);
        productMapper.updateById(product);
    }
}
