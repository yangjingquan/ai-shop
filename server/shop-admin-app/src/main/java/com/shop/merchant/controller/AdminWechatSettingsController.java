package com.shop.merchant.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.merchant.dto.MerchantWechatSettingsVO;
import com.shop.merchant.dto.UpdateWechatSettingsRequest;
import com.shop.merchant.service.MerchantWechatConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wechat-settings")
@RequiredArgsConstructor
public class AdminWechatSettingsController {

    private final MerchantWechatConfigService service;

    @GetMapping
    public ApiResult<PageResult<MerchantWechatSettingsVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResult.success(service.page(page, size, keyword));
    }

    @GetMapping("/{merchantId}")
    public ApiResult<MerchantWechatSettingsVO> get(@PathVariable Long merchantId) {
        return ApiResult.success(service.getSettings(merchantId));
    }

    @OpLog(action = "MERCHANT_WECHAT_CONFIG_UPDATE", targetType = "MERCHANT")
    @PutMapping("/{merchantId}")
    public ApiResult<Void> update(@PathVariable Long merchantId,
                                  @RequestBody @Valid UpdateWechatSettingsRequest request) {
        service.updateSettings(merchantId, request);
        return ApiResult.success(null);
    }
}
