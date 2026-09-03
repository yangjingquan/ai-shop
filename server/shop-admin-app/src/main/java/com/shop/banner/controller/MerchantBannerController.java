package com.shop.banner.controller;

import com.shop.banner.dto.BannerSaveRequest;
import com.shop.banner.dto.BannerVO;
import com.shop.banner.service.BannerService;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant/banner")
@RequiredArgsConstructor
public class MerchantBannerController {

    private final BannerService bannerService;

    @PostMapping
    @RequirePermission("merchant:banner:create")
    public ApiResult<Map<String, Long>> create(@RequestBody @Valid BannerSaveRequest req) {
        Long id = bannerService.create(currentMerchantId(), req);
        return ApiResult.success(Map.of("id", id));
    }

    @PutMapping("/{id}")
    @RequirePermission("merchant:banner:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid BannerSaveRequest req) {
        bannerService.update(currentMerchantId(), id, req);
        return ApiResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("merchant:banner:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        bannerService.delete(currentMerchantId(), id);
        return ApiResult.success(null);
    }

    @GetMapping("/page")
    @RequirePermission("merchant:banner:view")
    public ApiResult<PageResult<BannerVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(bannerService.page(currentMerchantId(), page, size));
    }

    private Long currentMerchantId() {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user.getMerchantId();
    }
}
