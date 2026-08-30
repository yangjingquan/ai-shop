package com.shop.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.dto.DashboardTrendVO;
import com.shop.dashboard.dto.DailyAmountRow;
import com.shop.dashboard.service.DashboardService;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
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
        Map<LocalDate, DailyAmountRow> paid = orderMapper.selectAdminDailyPaid(from).stream()
                .collect(Collectors.toMap(DailyAmountRow::getDay, Function.identity()));
        Map<LocalDate, DailyAmountRow> refunds = refundApplicationMapper.selectAdminDailyRefund(from).stream()
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
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LambdaQueryWrapper<Order> ordersToday = new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, today);
        scopeOrders(ordersToday, merchantId);
        vo.setOrderCountToday(orderMapper.selectCount(ordersToday));

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
        vo.setPaidAmountToday(sumPaidAmount(merchantId, today));
        return vo;
    }

    private BigDecimal sumPaidAmount(Long merchantId, LocalDateTime today) {
        QueryWrapper<Order> q = new QueryWrapper<>();
        q.select("COALESCE(SUM(pay_amount), 0)")
                .ge("pay_time", today)
                .eq("deleted", 0);
        if (merchantId != null) {
            q.eq("merchant_id", merchantId);
        }
        List<Object> values = orderMapper.selectObjs(q);
        if (values.isEmpty() || values.get(0) == null) {
            return BigDecimal.ZERO;
        }
        return values.get(0) instanceof BigDecimal
                ? (BigDecimal) values.get(0)
                : new BigDecimal(values.get(0).toString());
    }

    private long lowStockSkuCount(Long merchantId) {
        LambdaQueryWrapper<ProductSku> q = new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getActive, 1)
                .le(ProductSku::getStock, LOW_STOCK_THRESHOLD);
        List<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .select(Product::getId)
                        .eq(merchantId != null, Product::getMerchantId, merchantId)
                        .eq(Product::getStatus, 1)
                        .eq(Product::getAuditStatus, 1))
                .stream().map(Product::getId).toList();
        if (productIds.isEmpty()) {
            return 0;
        }
        q.in(ProductSku::getProductId, productIds);
        return productSkuMapper.selectCount(q);
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
