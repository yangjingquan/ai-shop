package com.shop.home.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.home.dto.HomeModuleUpdateRequest;
import com.shop.home.dto.HomeModuleVO;
import com.shop.home.service.HomeModuleConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/home/modules")
@RequiredArgsConstructor
public class MerchantHomeModuleController {
    private final HomeModuleConfigService homeModuleConfigService;

    @GetMapping
    @RequirePermission("merchant:home:config")
    public ApiResult<List<HomeModuleVO>> list() {
        return ApiResult.success(homeModuleConfigService.list(CurrentUserHolder.get().getMerchantId()));
    }

    @PutMapping
    @OpLog(action = "HOME_MODULE_CONFIG", targetType = "HOME_MODULE")
    @RequirePermission("merchant:home:config")
    public ApiResult<Void> update(@RequestBody @Valid HomeModuleUpdateRequest request) {
        homeModuleConfigService.update(CurrentUserHolder.get().getMerchantId(), request);
        return ApiResult.success(null);
    }
}
