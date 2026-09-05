package com.shop.bundle.controller;

import com.shop.bundle.dto.BundleActivityVO;
import com.shop.bundle.dto.BundleCartRequest;
import com.shop.bundle.dto.BundleCartResult;
import com.shop.bundle.dto.BundlePreviewRequest;
import com.shop.bundle.dto.BundlePreviewVO;
import com.shop.bundle.service.BundleService;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wx/bundles")
@RequiredArgsConstructor
public class WxBundleController {
    private final BundleService bundleService;
    private final WxMerchantResolver merchantResolver;

    @GetMapping("/product/{productId}")
    public ApiResult<BundleActivityVO> product(@PathVariable Long productId, HttpServletRequest request) {
        return ApiResult.success(bundleService.findActiveForProduct(merchantResolver.requireActiveMerchant(request), productId));
    }

    @GetMapping("/{id}")
    public ApiResult<BundleActivityVO> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(bundleService.publicGet(merchantResolver.requireActiveMerchant(request), id));
    }

    @PostMapping("/preview")
    public ApiResult<BundlePreviewVO> preview(@RequestBody @Valid BundlePreviewRequest request, HttpServletRequest httpRequest) {
        return ApiResult.success(bundleService.preview(merchantResolver.requireActiveMerchant(httpRequest), request));
    }

    @PostMapping("/cart")
    public ApiResult<BundleCartResult> addToCart(@RequestBody @Valid BundleCartRequest request, HttpServletRequest httpRequest) {
        return ApiResult.success(bundleService.addToCart(CurrentUserHolder.get().getUserId(), merchantResolver.requireActiveMerchant(httpRequest), request));
    }
}
