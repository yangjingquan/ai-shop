package com.shop.groupbuy.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.dto.GroupBuyGroupVO;
import com.shop.groupbuy.dto.GroupBuyProductDetailVO;
import com.shop.groupbuy.service.GroupBuyService;
import com.shop.product.dto.ProductListVO;
import com.shop.wx.config.WxMerchantResolver;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wx/group-buy")
@RequiredArgsConstructor
public class WxGroupBuyController {
    private final GroupBuyService groupBuyService;
    private final WxMerchantResolver wxMerchantResolver;
    private final MarketingFeatureService marketingFeatureService;

    @GetMapping("/products")
    public ApiResult<PageResult<ProductListVO>> products(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) String keyword,
                                                         HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        return ApiResult.success(groupBuyService.productPage(page, size, merchantId, categoryId, keyword));
    }

    @GetMapping("/products/{productId}")
    public ApiResult<GroupBuyProductDetailVO> productDetail(@PathVariable Long productId, HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        return ApiResult.success(groupBuyService.productDetail(productId, merchantId));
    }

    @PostMapping("/groups")
    public ApiResult<GroupBuyCreateVO> open(@RequestBody @Valid GroupBuyCreateRequest req, HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        return ApiResult.success(groupBuyService.openGroup(CurrentUserHolder.get().getUserId(), merchantId, req));
    }

    @PostMapping("/groups/{groupId}/join")
    public ApiResult<GroupBuyCreateVO> join(@PathVariable Long groupId, @RequestBody @Valid GroupBuyCreateRequest req,
                                            HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        return ApiResult.success(groupBuyService.joinGroup(CurrentUserHolder.get().getUserId(), merchantId, groupId, req));
    }

    @GetMapping("/groups/{groupId}")
    public ApiResult<GroupBuyGroupVO> group(@PathVariable Long groupId, HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.GROUP_BUY);
        return ApiResult.success(groupBuyService.groupDetail(groupId, merchantId));
    }
}
