package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.product.dto.MerchantCategoryVO;
import com.shop.product.service.MerchantCategoryService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final MerchantCategoryService merchantCategoryService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping("/tree")
    public ApiResult<List<MerchantCategoryVO>> tree(HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.currentMerchantId(request);
        return ApiResult.success(merchantCategoryService.tree(merchantId, true));
    }
}
