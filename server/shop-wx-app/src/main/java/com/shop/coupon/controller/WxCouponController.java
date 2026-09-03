package com.shop.coupon.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.aop.OpLog;
import com.shop.common.security.CurrentUserHolder;
import com.shop.coupon.dto.CouponVO;
import com.shop.coupon.dto.NewUserCouponEligibilityVO;
import com.shop.coupon.service.CouponService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wx/coupons")
@RequiredArgsConstructor
public class WxCouponController {
    private final CouponService couponService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping
    public ApiResult<List<CouponVO>> list(@RequestParam(required = false) Integer status,
                                         HttpServletRequest request) {
        return ApiResult.success(couponService.listUserCoupons(
                CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request), status));
    }

    @GetMapping("/new-user/eligibility")
    public ApiResult<NewUserCouponEligibilityVO> eligibility(HttpServletRequest request) {
        return ApiResult.success(couponService.eligibility(
                CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request)));
    }

    @PostMapping("/{templateId}/receive")
    @OpLog(action = "COUPON_RECEIVE", targetType = "COUPON_TEMPLATE", targetIdExpr = "#templateId")
    public ApiResult<Long> receive(@PathVariable Long templateId, HttpServletRequest request) {
        return ApiResult.success(couponService.receiveNewUserCoupon(
                CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request), templateId));
    }
}
