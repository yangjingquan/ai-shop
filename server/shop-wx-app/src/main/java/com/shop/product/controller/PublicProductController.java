package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public ApiResult<PageResult<ProductListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        // 公共视角：merchantId=null 触发 service 默认 status=1 过滤
        return ApiResult.success(productService.page(page, size, null, categoryId, keyword, null));
    }

    @GetMapping("/{id}")
    public ApiResult<ProductDetailVO> get(@PathVariable Long id) {
        return ApiResult.success(productService.get(id, null));
    }
}
