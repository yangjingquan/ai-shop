package com.shop.presale.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.presale.dto.*;
import com.shop.presale.service.PresaleService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wx/presales")
@RequiredArgsConstructor
public class WxPresaleController {
    private final PresaleService presaleService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping("/active")
    public ApiResult<List<PresaleActivityVO>> active(HttpServletRequest request) {
        return ApiResult.success(presaleService.active(wxMerchantResolver.currentMerchantId(request)));
    }

    @GetMapping("/{activityId}")
    public ApiResult<PresaleActivityVO> detail(@PathVariable Long activityId, HttpServletRequest request) {
        return ApiResult.success(presaleService.detail(wxMerchantResolver.currentMerchantId(request), activityId));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResult<PresaleOrderVO> order(@PathVariable String orderNo, HttpServletRequest request) {
        return ApiResult.success(presaleService.order(CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request), orderNo));
    }

    @PostMapping("/deposit-orders")
    public ApiResult<com.shop.order.dto.OrderCreateVO> createDeposit(@RequestBody @Valid PresaleDepositOrderRequest request, HttpServletRequest servletRequest) {
        return ApiResult.success(presaleService.createDepositOrder(CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(servletRequest), request));
    }

    @PostMapping("/{orderNo}/balance-orders")
    public ApiResult<com.shop.order.dto.OrderCreateVO> createBalance(@PathVariable String orderNo, HttpServletRequest request) {
        return ApiResult.success(presaleService.createBalanceOrder(CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request), orderNo));
    }

    @PostMapping("/{orderNo}/refund")
    public ApiResult<Void> refund(@PathVariable String orderNo, HttpServletRequest request) {
        presaleService.refundDeposit(CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request), orderNo);
        return ApiResult.success(null);
    }

    @PostMapping("/{orderNo}/address")
    public ApiResult<Void> updateAddress(@PathVariable String orderNo, @RequestBody @Valid PresaleAddressRequest body, HttpServletRequest request) {
        presaleService.updateAddress(CurrentUserHolder.get().getUserId(), wxMerchantResolver.requireActiveMerchant(request), orderNo, body.getAddressId());
        return ApiResult.success(null);
    }
}
