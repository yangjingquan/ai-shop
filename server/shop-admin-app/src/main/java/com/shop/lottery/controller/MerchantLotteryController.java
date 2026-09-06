package com.shop.lottery.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.lottery.dto.*;
import com.shop.lottery.service.LotteryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/lottery-activities")
@RequiredArgsConstructor
public class MerchantLotteryController {
    private final LotteryService lotteryService;

    @GetMapping
    @RequirePermission("merchant:lottery:view")
    public ApiResult<List<LotteryActivityVO>> list() { return ApiResult.success(lotteryService.merchantList(merchantId())); }

    @GetMapping("/{id}")
    @RequirePermission("merchant:lottery:view")
    public ApiResult<LotteryActivityVO> get(@PathVariable Long id) { return ApiResult.success(lotteryService.merchantGet(merchantId(), id)); }

    @PostMapping
    @OpLog(action = "LOTTERY_ACTIVITY_CREATE", targetType = "LOTTERY_ACTIVITY")
    @RequirePermission("merchant:lottery:create")
    public ApiResult<Long> create(@RequestBody @Valid LotteryActivitySaveRequest body) { return ApiResult.success(lotteryService.create(merchantId(), body)); }

    @PutMapping("/{id}")
    @OpLog(action = "LOTTERY_ACTIVITY_UPDATE", targetType = "LOTTERY_ACTIVITY", targetIdExpr = "#id")
    @RequirePermission("merchant:lottery:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid LotteryActivitySaveRequest body) { lotteryService.update(merchantId(), id, body); return ApiResult.success(null); }

    @PutMapping("/{id}/status")
    @OpLog(action = "LOTTERY_ACTIVITY_STATUS", targetType = "LOTTERY_ACTIVITY", targetIdExpr = "#id")
    @RequirePermission("merchant:lottery:status")
    public ApiResult<Void> status(@PathVariable Long id, @RequestParam Integer status) { lotteryService.updateStatus(merchantId(), id, status); return ApiResult.success(null); }

    @GetMapping("/{id}/stats")
    @RequirePermission("merchant:lottery:view")
    public ApiResult<LotteryStatsVO> stats(@PathVariable Long id) { return ApiResult.success(lotteryService.stats(merchantId(), id)); }

    @GetMapping("/{id}/rewards")
    @RequirePermission("merchant:lottery:view")
    public ApiResult<List<LotteryRewardVO>> rewards(@PathVariable Long id) { return ApiResult.success(lotteryService.merchantRewards(merchantId(), id)); }

    @PutMapping("/{activityId}/rewards/{rewardId}/status")
    @OpLog(action = "LOTTERY_REWARD_STATUS", targetType = "LOTTERY_REWARD", targetIdExpr = "#rewardId")
    @RequirePermission("merchant:lottery:reward")
    public ApiResult<Void> rewardStatus(@PathVariable Long activityId, @PathVariable Long rewardId, @RequestParam Integer status) { lotteryService.updateRewardStatus(merchantId(), activityId, rewardId, status); return ApiResult.success(null); }

    private Long merchantId() { CurrentUser user = CurrentUserHolder.get(); if (user == null || user.getMerchantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN); return user.getMerchantId(); }
}
