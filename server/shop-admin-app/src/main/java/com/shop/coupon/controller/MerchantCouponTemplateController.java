package com.shop.coupon.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.coupon.dto.CouponTemplateSaveRequest;
import com.shop.coupon.dto.CouponTemplateVO;
import com.shop.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/marketing/coupons/templates")
@RequiredArgsConstructor
public class MerchantCouponTemplateController {
    private final CouponService couponService;

    @GetMapping
    @RequirePermission("merchant:coupon:view")
    public ApiResult<List<CouponTemplateVO>> list() {
        return ApiResult.success(couponService.listTemplates(currentMerchantId()));
    }

    @PostMapping
    @OpLog(action = "COUPON_TEMPLATE_CREATE", targetType = "COUPON_TEMPLATE")
    @RequirePermission("merchant:coupon:create")
    public ApiResult<Map<String, Long>> create(@RequestBody @Valid CouponTemplateSaveRequest request) {
        return ApiResult.success(Map.of("id", couponService.createTemplate(currentMerchantId(), request)));
    }

    @PutMapping("/{id}")
    @OpLog(action = "COUPON_TEMPLATE_UPDATE", targetType = "COUPON_TEMPLATE", targetIdExpr = "#id")
    @RequirePermission("merchant:coupon:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid CouponTemplateSaveRequest request) {
        couponService.updateTemplate(currentMerchantId(), id, request);
        return ApiResult.success();
    }

    @PutMapping("/{id}/status")
    @OpLog(action = "COUPON_TEMPLATE_STATUS", targetType = "COUPON_TEMPLATE", targetIdExpr = "#id")
    @RequirePermission("merchant:coupon:status")
    public ApiResult<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateTemplateStatus(currentMerchantId(), id, status);
        return ApiResult.success();
    }

    private Long currentMerchantId() {
        CurrentUser current = CurrentUserHolder.get();
        if (current == null || current.getMerchantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        return current.getMerchantId();
    }
}
