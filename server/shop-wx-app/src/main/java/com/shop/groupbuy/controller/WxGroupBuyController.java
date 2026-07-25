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

    @GetMapping("/products")
    public ApiResult<PageResult<ProductListVO>> products(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) String keyword,
                                                         HttpServletRequest request) {
        return ApiResult.success(groupBuyService.productPage(page, size,
                wxMerchantResolver.currentMerchantId(request), categoryId, keyword));
    }

    @GetMapping("/products/{productId}")
    public ApiResult<GroupBuyProductDetailVO> productDetail(@PathVariable Long productId, HttpServletRequest request) {
        return ApiResult.success(groupBuyService.productDetail(productId, wxMerchantResolver.currentMerchantId(request)));
    }

    @PostMapping("/groups")
    public ApiResult<GroupBuyCreateVO> open(@RequestBody @Valid GroupBuyCreateRequest req) {
        return ApiResult.success(groupBuyService.openGroup(CurrentUserHolder.get().getUserId(), req));
    }

    @PostMapping("/groups/{groupId}/join")
    public ApiResult<GroupBuyCreateVO> join(@PathVariable Long groupId, @RequestBody @Valid GroupBuyCreateRequest req) {
        return ApiResult.success(groupBuyService.joinGroup(CurrentUserHolder.get().getUserId(), groupId, req));
    }

    @GetMapping("/groups/{groupId}")
    public ApiResult<GroupBuyGroupVO> group(@PathVariable Long groupId) {
        return ApiResult.success(groupBuyService.groupDetail(groupId));
    }
}
