package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping("/page")
    public ApiResult<PageResult<ProductListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isRecommend,
            HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        return ApiResult.success(productService.publicPage(page, size, merchantId, categoryId, keyword, isRecommend));
    }

    @GetMapping("/{id}")
    public ApiResult<ProductDetailVO> get(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        return ApiResult.success(productService.publicGet(id, merchantId));
    }
}
