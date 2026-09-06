package com.shop.presale.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.presale.dto.*;
import com.shop.presale.service.PresaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/presales")
@RequiredArgsConstructor
public class MerchantPresaleController {
    private final PresaleService presaleService;

    @GetMapping
    @RequirePermission("merchant:presale:view")
    public ApiResult<PageResult<PresaleActivityVO>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(presaleService.merchantPage(merchantId(), page, size));
    }

    @GetMapping("/{activityId}")
    @RequirePermission("merchant:presale:view")
    public ApiResult<PresaleActivityVO> get(@PathVariable Long activityId) { return ApiResult.success(presaleService.merchantGet(merchantId(), activityId)); }

    @PostMapping
    @RequirePermission("merchant:presale:create")
    public ApiResult<Long> create(@RequestBody @Valid PresaleActivitySaveRequest request) { return ApiResult.success(presaleService.saveActivity(merchantId(), userId(), request)); }

    @PutMapping("/{activityId}")
    @RequirePermission("merchant:presale:update")
    public ApiResult<Void> update(@PathVariable Long activityId, @RequestBody @Valid PresaleActivitySaveRequest request) { presaleService.updateActivity(merchantId(), userId(), activityId, request); return ApiResult.success(); }

    @GetMapping("/{activityId}/stats")
    @RequirePermission("merchant:presale:view")
    public ApiResult<PresaleStatsVO> stats(@PathVariable Long activityId) { return ApiResult.success(presaleService.stats(merchantId(), activityId)); }

    private Long merchantId() { CurrentUser user = CurrentUserHolder.get(); return user == null ? null : user.getMerchantId(); }
    private Long userId() { CurrentUser user = CurrentUserHolder.get(); return user == null ? null : user.getUserId(); }
}
