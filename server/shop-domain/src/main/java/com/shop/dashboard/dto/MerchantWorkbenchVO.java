package com.shop.dashboard.dto;

import com.shop.inventory.dto.InventorySkuVO;
import com.shop.order.dto.OrderListVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantWorkbenchVO {

    private DashboardOverviewVO overview;
    private MerchantWorkbenchTodoVO todo;
    private List<OrderListVO> recentOrders;
    private List<InventorySkuVO> lowStockSkus;
    private LocalDateTime generatedAt;
}
