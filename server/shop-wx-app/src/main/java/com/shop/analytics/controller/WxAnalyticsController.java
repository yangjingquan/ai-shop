package com.shop.analytics.controller;

import com.shop.analytics.dto.AnalyticsRequest;
import com.shop.analytics.service.BusinessAnalysisService;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/wx/analytics") @RequiredArgsConstructor
public class WxAnalyticsController {
    private final BusinessAnalysisService service; private final WxMerchantResolver merchantResolver;
    @PostMapping("/product-view") public ApiResult<Void> productView(@Valid @RequestBody AnalyticsRequest.ProductView request, HttpServletRequest http) {
        service.recordProductView(merchantResolver.requireActiveMerchant(http), CurrentUserHolder.get().getUserId(), request.getEventId(), request.getProductId(), request.getChannelCode()); return ApiResult.success();
    }
}
