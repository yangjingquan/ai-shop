package com.shop.inventory.service;

import com.shop.common.response.PageResult;
import com.shop.inventory.dto.InventoryAdjustmentRequest;
import com.shop.inventory.dto.InventorySkuVO;
import com.shop.inventory.dto.InventoryTransactionVO;

public interface InventoryService {

    PageResult<InventorySkuVO> skuPage(Long merchantId, int page, int size,
                                       String keyword, boolean lowStockOnly, int threshold);

    PageResult<InventoryTransactionVO> transactionPage(Long merchantId, int page, int size, Long skuId);

    void adjust(Long merchantId, Long operatorId, InventoryAdjustmentRequest request);
}
