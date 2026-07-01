package com.shop.merchant.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.merchant.dto.MerchantSelfVO;
import com.shop.merchant.dto.UpdateMerchantSelfRequest;
import com.shop.merchant.service.MerchantManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/profile")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final MerchantManagementService merchantManagementService;

    @GetMapping
    public ApiResult<MerchantSelfVO> getSelf() {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        return ApiResult.success(merchantManagementService.getSelf(merchantId));
    }

    @PutMapping
    public ApiResult<Void> updateSelf(@RequestBody @Valid UpdateMerchantSelfRequest req) {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        merchantManagementService.updateSelf(merchantId, req);
        return ApiResult.success(null);
    }
}
