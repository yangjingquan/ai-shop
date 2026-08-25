package com.shop.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardOverviewVO {

    private Long merchantCount;
    private Long activeMerchantCount;
    private Long orderCountToday;
    private BigDecimal paidAmountToday = BigDecimal.ZERO;
    private Long pendingShipCount;
    private Long pendingRefundCount;
    private Long onSaleProductCount;
    private Long lowStockSkuCount;
}
