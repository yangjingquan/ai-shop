package com.shop.bundle.controller;

import com.shop.bundle.dto.BundleActivityRequest;
import com.shop.bundle.dto.BundleActivityVO;
import com.shop.bundle.service.BundleService;
import com.shop.common.aop.OpLog;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/bundles")
@RequiredArgsConstructor
public class MerchantBundleController {
    private final BundleService bundleService;

    @GetMapping
    @RequirePermission("merchant:marketing:view")
    public ApiResult<List<BundleActivityVO>> list() { return ApiResult.success(bundleService.merchantList(merchantId())); }

    @GetMapping("/{id}")
    @RequirePermission("merchant:marketing:view")
    public ApiResult<BundleActivityVO> get(@PathVariable Long id) { return ApiResult.success(bundleService.merchantGet(merchantId(), id)); }

    @PostMapping
    @OpLog(action = "BUNDLE_CREATE", targetType = "BUNDLE_ACTIVITY")
    @RequirePermission("merchant:marketing:feature:update")
    public ApiResult<Long> create(@RequestBody @Valid BundleActivityRequest request) {
        return ApiResult.success(bundleService.save(merchantId(), userId(), null, request));
    }

    @PutMapping("/{id}")
    @OpLog(action = "BUNDLE_UPDATE", targetType = "BUNDLE_ACTIVITY", targetIdExpr = "#id")
    @RequirePermission("merchant:marketing:feature:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid BundleActivityRequest request) {
        bundleService.save(merchantId(), userId(), id, request); return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @OpLog(action = "BUNDLE_DISABLE", targetType = "BUNDLE_ACTIVITY", targetIdExpr = "#id")
    @RequirePermission("merchant:marketing:feature:update")
    public ApiResult<Void> delete(@PathVariable Long id) { bundleService.delete(merchantId(), id); return ApiResult.success(); }

    private Long merchantId() { CurrentUser u = CurrentUserHolder.get(); if (u == null || u.getMerchantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN); return u.getMerchantId(); }
    private Long userId() { CurrentUser u = CurrentUserHolder.get(); return u == null ? null : u.getUserId(); }
}
