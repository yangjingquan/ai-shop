package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.dto.ProductSaveRequest;
import com.shop.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody ProductSaveRequest req) {
        return ApiResult.success(productService.create(req, currentMerchantId()));
    }

    @GetMapping
    public ApiResult<PageResult<ProductListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResult.success(productService.page(page, size, currentMerchantId(),
                categoryId, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResult<ProductDetailVO> get(@PathVariable Long id) {
        return ApiResult.success(productService.get(id, currentMerchantId()));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest req) {
        productService.update(id, req, currentMerchantId());
        return ApiResult.success();
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> setStatus(@PathVariable Long id, @RequestParam int status) {
        productService.setStatus(id, status, currentMerchantId());
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        productService.delete(id, currentMerchantId());
        return ApiResult.success();
    }

    private Long currentMerchantId() {
        CurrentUser u = CurrentUserHolder.get();
        return u == null ? null : u.getMerchantId();
    }
}
