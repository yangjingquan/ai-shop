package com.shop.marketing.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.pricing.dto.PricingRuleVersionRequest;
import com.shop.pricing.dto.PricingRuleVersionVO;
import com.shop.pricing.service.PricingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-only governance for the active quote rule version. */
@RestController
@RequestMapping("/api/admin/marketing-rules")
@RequiredArgsConstructor
public class AdminPricingRuleController {
    private final PricingRuleService pricingRuleService;

    @GetMapping("/active")
    public ApiResult<PricingRuleVersionVO> active() { return ApiResult.success(pricingRuleService.activeView()); }

    @PutMapping("/publish")
    public ApiResult<PricingRuleVersionVO> publish(@RequestBody @Valid PricingRuleVersionRequest request) {
        CurrentUser user = CurrentUserHolder.get();
        return ApiResult.success(pricingRuleService.publish(user == null ? null : user.getUserId(), request));
    }
}
