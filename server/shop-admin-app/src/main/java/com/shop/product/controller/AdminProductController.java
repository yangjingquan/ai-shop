package com.shop.product.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/audit/page")
    public ApiResult<PageResult<ProductListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long merchantId) {
        return ApiResult.success(productService.adminAuditPage(page, size, auditStatus, keyword, merchantId));
    }

    @Data
    public static class AuditRequest {
        @NotNull
        private Integer auditStatus;
        private String auditReason;
    }

    @OpLog(action = "PRODUCT_AUDIT", targetType = "PRODUCT", targetIdExpr = "#productId")
    @PutMapping("/{productId}/audit")
    public ApiResult<Void> audit(@PathVariable Long productId, @Valid @RequestBody AuditRequest req) {
        CurrentUser user = CurrentUserHolder.get();
        Long adminId = user == null ? null : user.getUserId();
        productService.audit(productId, req.getAuditStatus(), req.getAuditReason(), adminId);
        return ApiResult.success(null);
    }

    @Data
    public static class ForceOfflineRequest {
        @NotNull
        @Size(max = 255)
        private String reason;
    }

    @OpLog(action = "PRODUCT_FORCE_OFFLINE", targetType = "PRODUCT", targetIdExpr = "#productId")
    @PostMapping("/{productId}/force-offline")
    public ApiResult<Void> forceOffline(@PathVariable Long productId,
                                         @Valid @RequestBody ForceOfflineRequest req) {
        CurrentUser user = CurrentUserHolder.get();
        Long adminId = user == null ? null : user.getUserId();
        productService.forceOffline(productId, req.getReason(), adminId);
        return ApiResult.success(null);
    }
}
