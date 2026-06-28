package com.shop.merchant.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.merchant.dto.CreateMerchantRequest;
import com.shop.merchant.dto.MerchantVO;
import com.shop.merchant.dto.SetStatusRequest;
import com.shop.merchant.dto.UpdateMerchantRequest;
import com.shop.merchant.service.MerchantManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantManagementService service;

    @PostMapping
    public ApiResult<Map<String, Long>> create(@RequestBody @Valid CreateMerchantRequest req) {
        CurrentUser cu = CurrentUserHolder.get();
        Long id = service.createMerchant(req, cu.getUserId());
        return ApiResult.success(Map.of("merchantId", id));
    }

    @GetMapping
    public ApiResult<PageResult<MerchantVO>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String keyword) {
        return ApiResult.success(service.listMerchants(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResult<MerchantVO> get(@PathVariable Long id) {
        return ApiResult.success(service.getMerchant(id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateMerchantRequest req) {
        service.updateMerchant(id, req);
        return ApiResult.success(null);
    }

    @OpLog(action = "MERCHANT_STATUS_CHANGE", targetType = "MERCHANT")
    @PutMapping("/{id}/status")
    public ApiResult<Void> setStatus(@PathVariable Long id, @RequestBody @Valid SetStatusRequest req) {
        service.setStatus(id, req.getStatus());
        return ApiResult.success(null);
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String newPassword;
    }

    @OpLog(action = "MERCHANT_RESET_PWD", targetType = "MERCHANT")
    @PutMapping("/{id}/password")
    public ApiResult<Void> resetPassword(@PathVariable Long id,
                                         @RequestBody @Valid ResetPasswordRequest req) {
        service.resetPassword(id, req.getNewPassword());
        return ApiResult.success(null);
    }
}
