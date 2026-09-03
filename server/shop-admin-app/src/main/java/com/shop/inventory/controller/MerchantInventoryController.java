package com.shop.inventory.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.inventory.dto.InventoryAdjustmentRequest;
import com.shop.inventory.dto.InventorySkuVO;
import com.shop.inventory.dto.InventoryTransactionVO;
import com.shop.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/inventory")
@RequiredArgsConstructor
public class MerchantInventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/skus")
    @RequirePermission("merchant:inventory:view")
    public ApiResult<PageResult<InventorySkuVO>> skus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(defaultValue = "5") int threshold) {
        return ApiResult.success(inventoryService.skuPage(merchantId(), page, size, keyword, lowStockOnly, threshold));
    }

    @GetMapping("/transactions")
    @RequirePermission("merchant:inventory:transaction:view")
    public ApiResult<PageResult<InventoryTransactionVO>> transactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long skuId) {
        return ApiResult.success(inventoryService.transactionPage(merchantId(), page, size, skuId));
    }

    @PostMapping("/adjust")
    @RequirePermission("merchant:inventory:adjust")
    public ApiResult<Void> adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
        CurrentUser user = CurrentUserHolder.get();
        inventoryService.adjust(merchantId(), user == null ? null : user.getUserId(), request);
        return ApiResult.success();
    }

    private Long merchantId() {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user.getMerchantId();
    }
}
