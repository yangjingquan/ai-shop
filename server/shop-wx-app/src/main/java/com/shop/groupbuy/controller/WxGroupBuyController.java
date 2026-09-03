package com.shop.groupbuy.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.dto.GroupBuyGroupVO;
import com.shop.groupbuy.dto.GroupBuyProductDetailVO;
import com.shop.groupbuy.dto.GroupBuySubscribeRequest;
import com.shop.groupbuy.dto.GroupBuySubscriptionConfigVO;
import com.shop.groupbuy.dto.GroupBuyShareEventRequest;
import com.shop.groupbuy.service.GroupBuyService;
import com.shop.groupbuy.service.GroupBuyMessageService;
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
    private final GroupBuyMessageService groupBuyMessageService;

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
        return ApiResult.success(groupBuyService.groupDetail(groupId, merchantId));
    }

    @GetMapping("/groups/{groupId}/subscribe-config")
    public ApiResult<GroupBuySubscriptionConfigVO> subscribeConfig(@PathVariable Long groupId,
                                                                    HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        return ApiResult.success(groupBuyMessageService.subscriptionConfig(merchantId, groupId));
    }

    @PostMapping("/subscribe")
    public ApiResult<Void> subscribe(@RequestBody @Valid GroupBuySubscribeRequest req,
                                     HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        groupBuyMessageService.recordSubscriptions(CurrentUserHolder.get().getUserId(), merchantId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/share-events")
    public ApiResult<Void> shareEvent(@RequestBody @Valid GroupBuyShareEventRequest req,
                                      HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        groupBuyMessageService.recordShare(CurrentUserHolder.get().getUserId(), merchantId, req.getGroupId(),
                req.getSource(), Boolean.TRUE.equals(req.getOpened()));
        return ApiResult.success(null);
    }
}
