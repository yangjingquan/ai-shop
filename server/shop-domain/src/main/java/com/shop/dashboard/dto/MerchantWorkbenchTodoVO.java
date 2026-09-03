package com.shop.dashboard.dto;

import lombok.Data;

@Data
public class MerchantWorkbenchTodoVO {

    private Long pendingShipCount;
    private Long pendingRefundCount;
    private Long pendingReturnReceiveCount;
    private Long failedRefundCount;
    private Long lowStockSkuCount;
}
