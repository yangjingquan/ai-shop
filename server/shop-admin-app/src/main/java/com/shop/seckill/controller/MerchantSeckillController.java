package com.shop.seckill.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.seckill.dto.SeckillActivitySaveRequest;
import com.shop.seckill.dto.SeckillActivityVO;
import com.shop.seckill.service.SeckillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/seckill")
@RequiredArgsConstructor
public class MerchantSeckillController {
    private final SeckillService seckillService;

    @GetMapping
    @RequirePermission("merchant:seckill:view")
    public ApiResult<PageResult<SeckillActivityVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(seckillService.merchantPage(merchantId(), page, size));
    }

    @GetMapping("/{activityId}")
    @RequirePermission("merchant:seckill:view")
    public ApiResult<SeckillActivityVO> get(@PathVariable Long activityId) {
        return ApiResult.success(seckillService.merchantGet(merchantId(), activityId));
    }

    @PostMapping
    @RequirePermission("merchant:seckill:create")
    public ApiResult<Long> create(@RequestBody @Valid SeckillActivitySaveRequest request) {
        return ApiResult.success(seckillService.saveActivity(merchantId(), userId(), request));
    }

    @PutMapping("/{activityId}")
    @RequirePermission("merchant:seckill:update")
    public ApiResult<Void> update(@PathVariable Long activityId,
                                  @RequestBody @Valid SeckillActivitySaveRequest request) {
        seckillService.updateActivity(merchantId(), userId(), activityId, request);
        return ApiResult.success();
    }

    private Long merchantId() {
        CurrentUser user = CurrentUserHolder.get();
        return user == null ? null : user.getMerchantId();
    }

    private Long userId() {
        CurrentUser user = CurrentUserHolder.get();
        return user == null ? null : user.getUserId();
    }
}
