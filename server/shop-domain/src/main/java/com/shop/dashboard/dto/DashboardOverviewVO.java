package com.shop.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardOverviewVO {

    private Long merchantCount;
    private Long activeMerchantCount;
    private Long orderCountToday;
    /** 今日创建订单数，按 order.created_at 统计，仅用于运营量级观察。 */
    private Long createdOrderCountToday;
    /** 今日支付成功订单数，按 payment_log.created_at 统计。 */
    private Long paidOrderCountToday;
    private BigDecimal paidAmountToday = BigDecimal.ZERO;
    /** 今日退款成功金额，按 refund_application.refund_time 统计。 */
    private BigDecimal refundAmountToday = BigDecimal.ZERO;
    /** 今日净额 = 今日支付成功金额 - 今日退款成功金额。 */
    private BigDecimal netAmountToday = BigDecimal.ZERO;
    private Long pendingShipCount;
    private Long pendingRefundCount;
    private Long onSaleProductCount;
    private Long lowStockSkuCount;
}
