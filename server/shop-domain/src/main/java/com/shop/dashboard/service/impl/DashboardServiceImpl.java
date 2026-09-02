package com.shop.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.dto.DashboardTrendVO;
import com.shop.dashboard.dto.DailyAmountRow;
import com.shop.dashboard.service.DashboardService;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public DashboardOverviewVO adminOverview() {
        DashboardOverviewVO vo = overview(null);
        vo.setMerchantCount(merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()));
        vo.setActiveMerchantCount(merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, 1)));
        return vo;
    }

    @Override
    public DashboardOverviewVO merchantOverview(Long merchantId) {
        return overview(merchantId);
    }

    @Override
    public List<DashboardTrendVO> adminTrend(int days) {
        int safeDays = Math.min(Math.max(days, 7), 90);
        LocalDate firstDay = LocalDate.now().minusDays(safeDays - 1L);
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        Map<LocalDate, DailyAmountRow> paid = paymentLogMapper.selectAdminDailyPaid(from, to).stream()
                .collect(Collectors.toMap(DailyAmountRow::getDay, Function.identity()));
        Map<LocalDate, DailyAmountRow> refunds = refundApplicationMapper.selectAdminDailyRefund(from, to).stream()
                .collect(Collectors.toMap(DailyAmountRow::getDay, Function.identity()));
        return java.util.stream.IntStream.range(0, safeDays).mapToObj(offset -> {
            LocalDate day = firstDay.plusDays(offset);
            DailyAmountRow paidRow = paid.get(day);
            DailyAmountRow refundRow = refunds.get(day);
            long count = paidRow == null || paidRow.getCount() == null ? 0 : paidRow.getCount();
            BigDecimal paidAmount = paidRow == null || paidRow.getAmount() == null ? BigDecimal.ZERO : paidRow.getAmount();
            BigDecimal refundAmount = refundRow == null || refundRow.getAmount() == null ? BigDecimal.ZERO : refundRow.getAmount();
            return new DashboardTrendVO(day.toString(), count, paidAmount, refundAmount,
                    paidAmount.subtract(refundAmount));
        }).toList();
    }

    private DashboardOverviewVO overview(Long merchantId) {
        LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime tomorrow = today.plusDays(1);
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LambdaQueryWrapper<Order> ordersToday = new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, today)
                .lt(Order::getCreatedAt, tomorrow);
        scopeOrders(ordersToday, merchantId);
        Long createdOrderCount = orderMapper.selectCount(ordersToday);
        vo.setOrderCountToday(createdOrderCount);
        vo.setCreatedOrderCountToday(createdOrderCount);
        vo.setPaidOrderCountToday(nullToZero(paymentLogMapper.selectPaidCount(today, tomorrow, merchantId)));

        LambdaQueryWrapper<Order> pendingShip = new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 6);
        scopeOrders(pendingShip, merchantId);
        vo.setPendingShipCount(orderMapper.selectCount(pendingShip));

        LambdaQueryWrapper<RefundApplication> refunds = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getStatus, 0);
        scopeRefunds(refunds, merchantId);
        vo.setPendingRefundCount(refundApplicationMapper.selectCount(refunds));

        LambdaQueryWrapper<Product> products = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .eq(Product::getAuditStatus, 1);
        scopeProducts(products, merchantId);
        vo.setOnSaleProductCount(productMapper.selectCount(products));

        vo.setLowStockSkuCount(lowStockSkuCount(merchantId));
        BigDecimal paidAmount = paymentLogMapper.selectPaidAmount(today, tomorrow, merchantId);
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        vo.setPaidAmountToday(paidAmount);
        BigDecimal refundAmount = refundApplicationMapper.selectSuccessfulRefundAmount(today, tomorrow, merchantId);
        if (refundAmount == null) refundAmount = BigDecimal.ZERO;
        vo.setRefundAmountToday(refundAmount);
        vo.setNetAmountToday(paidAmount.subtract(refundAmount));
        return vo;
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private long lowStockSkuCount(Long merchantId) {
        return productSkuMapper.countLowStock(merchantId, LOW_STOCK_THRESHOLD);
    }

    private void scopeOrders(LambdaQueryWrapper<Order> q, Long merchantId) {
        if (merchantId != null) q.eq(Order::getMerchantId, merchantId);
    }

    private void scopeRefunds(LambdaQueryWrapper<RefundApplication> q, Long merchantId) {
        if (merchantId != null) q.eq(RefundApplication::getMerchantId, merchantId);
    }

    private void scopeProducts(LambdaQueryWrapper<Product> q, Long merchantId) {
        if (merchantId != null) q.eq(Product::getMerchantId, merchantId);
    }
}
