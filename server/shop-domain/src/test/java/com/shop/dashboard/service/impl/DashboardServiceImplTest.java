package com.shop.dashboard.service.impl;

import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.dto.MerchantWorkbenchVO;
import com.shop.inventory.dto.InventorySkuVO;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PaymentLogMapper paymentLogMapper;
    @Mock
    private RefundApplicationMapper refundApplicationMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;

    @Test
    void overviewUsesPaymentAndSuccessfulRefundLedgersForMoneyMetrics() {
        when(merchantMapper.selectCount(any())).thenReturn(8L, 6L);
        when(orderMapper.selectCount(any())).thenReturn(12L, 3L);
        when(paymentLogMapper.selectPaidCount(any(), any(), any())).thenReturn(7L);
        when(paymentLogMapper.selectPaidAmount(any(), any(), any())).thenReturn(new BigDecimal("128.50"));
        when(refundApplicationMapper.selectCount(any())).thenReturn(2L);
        when(refundApplicationMapper.selectSuccessfulRefundAmount(any(), any(), any()))
                .thenReturn(new BigDecimal("20.00"));
        when(productMapper.selectCount(any())).thenReturn(4L);
        when(productSkuMapper.countLowStock(any(), any(Integer.class))).thenReturn(0L);

        DashboardServiceImpl service = new DashboardServiceImpl(
                merchantMapper, orderMapper, paymentLogMapper, refundApplicationMapper,
                productMapper, productSkuMapper);

        DashboardOverviewVO result = service.adminOverview();

        assertEquals(12L, result.getCreatedOrderCountToday());
        assertEquals(7L, result.getPaidOrderCountToday());
        assertEquals(new BigDecimal("128.50"), result.getPaidAmountToday());
        assertEquals(new BigDecimal("20.00"), result.getRefundAmountToday());
        assertEquals(new BigDecimal("108.50"), result.getNetAmountToday());
    }

    @Test
    void merchantWorkbenchAggregatesActionableItemsAndPreviews() {
        when(orderMapper.selectCount(any())).thenReturn(5L, 2L);
        when(paymentLogMapper.selectPaidCount(any(), any(), any())).thenReturn(3L);
        when(paymentLogMapper.selectPaidAmount(any(), any(), any())).thenReturn(new BigDecimal("88.00"));
        when(refundApplicationMapper.selectSuccessfulRefundAmount(any(), any(), any()))
                .thenReturn(new BigDecimal("8.00"));
        when(refundApplicationMapper.selectCount(any())).thenReturn(2L, 1L, 3L);
        when(productMapper.selectCount(any())).thenReturn(6L);
        when(productSkuMapper.countLowStock(any(), any(Integer.class))).thenReturn(4L);

        Page<Order> orders = new Page<>(1, 5);
        Order order = new Order();
        order.setOrderNo("ORDER-1");
        order.setStatus(6);
        order.setPayAmount(new BigDecimal("19.90"));
        orders.setRecords(java.util.List.of(order));
        when(orderMapper.selectPage(any(), any())).thenReturn(orders);

        Page<InventorySkuVO> skus = new Page<>(1, 5);
        InventorySkuVO sku = new InventorySkuVO();
        sku.setSkuId(7L);
        sku.setProductName("低库存商品");
        sku.setStock(2);
        skus.setRecords(java.util.List.of(sku));
        when(productSkuMapper.selectMerchantLowStockPage(any(), any(), any(Integer.class))).thenReturn(skus);

        DashboardServiceImpl service = new DashboardServiceImpl(
                merchantMapper, orderMapper, paymentLogMapper, refundApplicationMapper,
                productMapper, productSkuMapper);

        MerchantWorkbenchVO result = service.merchantWorkbench(9L);

        assertEquals(4L, result.getTodo().getLowStockSkuCount());
        assertEquals(2L, result.getTodo().getPendingRefundCount());
        assertEquals(1L, result.getTodo().getPendingReturnReceiveCount());
        assertEquals(3L, result.getTodo().getFailedRefundCount());
        assertEquals("ORDER-1", result.getRecentOrders().get(0).getOrderNo());
        assertEquals(1, result.getLowStockSkus().size());
    }
}
