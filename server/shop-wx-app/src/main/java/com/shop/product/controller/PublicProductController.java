package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import com.shop.wx.config.WxMerchantResolver;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.bundle.service.BundleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final WxMerchantResolver wxMerchantResolver;
    private final MarketingFeatureService marketingFeatureService;
    private final BundleService bundleService;

    @GetMapping("/page")
    public ApiResult<PageResult<ProductListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isRecommend,
            @RequestParam(required = false) Integer isGroupBuy,
            HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        boolean groupBuyEnabled = marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        if (Integer.valueOf(1).equals(isGroupBuy)) {
            marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        }
        PageResult<ProductListVO> result = productService.publicPage(page, size, merchantId, categoryId, keyword,
                isRecommend, isGroupBuy);
        if (!groupBuyEnabled && !Integer.valueOf(1).equals(isGroupBuy)) {
            result.getList().forEach(this::hideGroupBuyFields);
        }
        return ApiResult.success(result);
    }

    @GetMapping("/{id}")
    public ApiResult<ProductDetailVO> get(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        ProductDetailVO product = productService.publicGet(id, merchantId);
        if (!marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.GROUP_BUY)) {
            hideGroupBuyFields(product);
        }
        if (marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.BUNDLE)) {
            product.setBundle(bundleService.findActiveForProduct(merchantId, id));
        }
        return ApiResult.success(product);
    }

    private void hideGroupBuyFields(ProductListVO product) {
        product.setIsGroupBuy(0);
        product.setGroupBuyPrice(null);
        product.setGroupBuyRequiredCount(null);
    }

    private void hideGroupBuyFields(ProductDetailVO product) {
        product.setIsGroupBuy(0);
        product.setGroupBuyPrice(null);
        product.setGroupBuyRequiredCount(null);
    }
}
