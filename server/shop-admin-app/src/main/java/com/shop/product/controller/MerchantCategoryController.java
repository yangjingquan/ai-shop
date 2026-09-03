package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.product.dto.CategoryVO;
import com.shop.product.dto.MerchantCategoryImportRequest;
import com.shop.product.dto.MerchantCategoryRequest;
import com.shop.product.dto.MerchantCategoryVO;
import com.shop.product.service.MerchantCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/categories")
@RequiredArgsConstructor
public class MerchantCategoryController {

    private final MerchantCategoryService merchantCategoryService;

    @GetMapping("/tree")
    @RequirePermission("merchant:category:view")
    public ApiResult<List<MerchantCategoryVO>> tree() {
        return ApiResult.success(merchantCategoryService.tree(currentMerchantId(), false));
    }

    @GetMapping("/enabled-tree")
    @RequirePermission("merchant:category:view")
    public ApiResult<List<MerchantCategoryVO>> enabledTree() {
        return ApiResult.success(merchantCategoryService.tree(currentMerchantId(), true));
    }

    @GetMapping("/platform-tree")
    @RequirePermission("merchant:category:view")
    public ApiResult<List<CategoryVO>> platformTree() {
        return ApiResult.success(merchantCategoryService.platformTree());
    }

    @PostMapping
    @RequirePermission("merchant:category:create")
    public ApiResult<Long> create(@Valid @RequestBody MerchantCategoryRequest req) {
        return ApiResult.success(merchantCategoryService.create(currentMerchantId(), req));
    }

    @PostMapping("/import")
    @RequirePermission("merchant:category:import")
    public ApiResult<Void> importFromPlatform(@Valid @RequestBody MerchantCategoryImportRequest req) {
        merchantCategoryService.importFromPlatform(currentMerchantId(), req);
        return ApiResult.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("merchant:category:update")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody MerchantCategoryRequest req) {
        merchantCategoryService.update(currentMerchantId(), id, req);
        return ApiResult.success();
    }

    @PutMapping("/{id}/status")
    @RequirePermission("merchant:category:status")
    public ApiResult<Void> setStatus(@PathVariable Long id, @RequestParam int status) {
        merchantCategoryService.setStatus(currentMerchantId(), id, status);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("merchant:category:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        merchantCategoryService.delete(currentMerchantId(), id);
        return ApiResult.success();
    }

    private Long currentMerchantId() {
        CurrentUser u = CurrentUserHolder.get();
        return u == null ? null : u.getMerchantId();
    }
}
