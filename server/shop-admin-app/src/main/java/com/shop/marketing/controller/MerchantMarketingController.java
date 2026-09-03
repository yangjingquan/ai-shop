package com.shop.marketing.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.marketing.dto.MarketingFeatureUpdateRequest;
import com.shop.marketing.dto.MarketingFeatureVO;
import com.shop.marketing.service.MarketingFeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/marketing")
@RequiredArgsConstructor
public class MerchantMarketingController {
    private final MarketingFeatureService marketingFeatureService;

    @GetMapping("/features")
    @RequirePermission("merchant:marketing:view")
    public ApiResult<List<MarketingFeatureVO>> features() {
        return ApiResult.success(marketingFeatureService.list(CurrentUserHolder.get().getMerchantId()));
    }

    @PutMapping("/features/{code}")
    @RequirePermission("merchant:marketing:feature:update")
    public ApiResult<Void> update(@PathVariable String code,
                                  @RequestBody @Valid MarketingFeatureUpdateRequest req) {
        marketingFeatureService.update(CurrentUserHolder.get().getMerchantId(), code, req.getEnabled(),
                CurrentUserHolder.get().getUserId());
        return ApiResult.success(null);
    }
}
