package com.shop.marketing.controller;

import com.shop.common.response.ApiResult;
import com.shop.marketing.dto.MarketingFeatureVO;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/marketing")
@RequiredArgsConstructor
public class PublicMarketingController {
    private final MarketingFeatureService marketingFeatureService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping("/features")
    public ApiResult<List<MarketingFeatureVO>> features(HttpServletRequest request) {
        return ApiResult.success(marketingFeatureService.listEnabled(wxMerchantResolver.currentMerchantId(request)));
    }
}
