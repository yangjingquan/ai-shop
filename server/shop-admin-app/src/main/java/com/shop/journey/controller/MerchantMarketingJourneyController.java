package com.shop.journey.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.journey.dto.MarketingJourneyRequest;
import com.shop.journey.dto.MarketingJourneyVO;
import com.shop.journey.service.MarketingJourneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/marketing-journeys")
@RequiredArgsConstructor
public class MerchantMarketingJourneyController {
    private final MarketingJourneyService marketingJourneyService;

    private Long merchant() { return CurrentUserHolder.get().getMerchantId(); }

    @GetMapping
    @RequirePermission("merchant:journey:view")
    public ApiResult<List<MarketingJourneyVO>> list() {
        return ApiResult.success(marketingJourneyService.list(merchant()));
    }

    @PostMapping
    @OpLog(action = "MARKETING_JOURNEY_CREATE", targetType = "MARKETING_JOURNEY")
    @RequirePermission("merchant:journey:manage")
    public ApiResult<Long> create(@Valid @RequestBody MarketingJourneyRequest request) {
        return ApiResult.success(marketingJourneyService.save(merchant(), null, request));
    }

    @PutMapping("/{id}")
    @OpLog(action = "MARKETING_JOURNEY_UPDATE", targetType = "MARKETING_JOURNEY", targetIdExpr = "#id")
    @RequirePermission("merchant:journey:manage")
    public ApiResult<Long> update(@PathVariable Long id, @Valid @RequestBody MarketingJourneyRequest request) {
        return ApiResult.success(marketingJourneyService.save(merchant(), id, request));
    }

    @PostMapping("/{id}/scan")
    @RequirePermission("merchant:journey:manage")
    public ApiResult<Void> scan(@PathVariable Long id) {
        marketingJourneyService.scan(merchant(), id);
        return ApiResult.success();
    }
}
