package com.shop.referral.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.referral.dto.*;
import com.shop.referral.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/referral-campaigns")
@RequiredArgsConstructor
public class MerchantReferralController {
    private final ReferralService referralService;

    @GetMapping
    @RequirePermission("merchant:referral:view")
    public ApiResult<List<ReferralCampaignVO>> list() { return ApiResult.success(referralService.merchantList(merchantId())); }

    @GetMapping("/{id}")
    @RequirePermission("merchant:referral:view")
    public ApiResult<ReferralCampaignVO> get(@PathVariable Long id) { return ApiResult.success(referralService.merchantGet(merchantId(), id)); }

    @PostMapping
    @OpLog(action = "REFERRAL_CAMPAIGN_CREATE", targetType = "REFERRAL_CAMPAIGN")
    @RequirePermission("merchant:referral:create")
    public ApiResult<Long> create(@RequestBody @Valid ReferralCampaignSaveRequest request) { return ApiResult.success(referralService.create(merchantId(), request)); }

    @PutMapping("/{id}")
    @OpLog(action = "REFERRAL_CAMPAIGN_UPDATE", targetType = "REFERRAL_CAMPAIGN", targetIdExpr = "#id")
    @RequirePermission("merchant:referral:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid ReferralCampaignSaveRequest request) { referralService.update(merchantId(), id, request); return ApiResult.success(); }

    @PutMapping("/{id}/status")
    @OpLog(action = "REFERRAL_CAMPAIGN_STATUS", targetType = "REFERRAL_CAMPAIGN", targetIdExpr = "#id")
    @RequirePermission("merchant:referral:status")
    public ApiResult<Void> status(@PathVariable Long id, @RequestParam Integer status) { referralService.updateStatus(merchantId(), id, status); return ApiResult.success(); }

    @GetMapping("/{id}/stats")
    @RequirePermission("merchant:referral:view")
    public ApiResult<ReferralStatsVO> stats(@PathVariable Long id) { return ApiResult.success(referralService.stats(merchantId(), id)); }

    @GetMapping("/{id}/relations")
    @RequirePermission("merchant:referral:view")
    public ApiResult<List<ReferralRelationVO>> relations(@PathVariable Long id) { return ApiResult.success(referralService.relations(merchantId(), id)); }

    @PostMapping("/{id}/relations/{relationId}/freeze")
    @OpLog(action = "REFERRAL_RELATION_FREEZE", targetType = "REFERRAL_RELATION", targetIdExpr = "#relationId")
    @RequirePermission("merchant:referral:relation:freeze")
    public ApiResult<Void> freeze(@PathVariable Long id, @PathVariable Long relationId) { referralService.freezeRelation(merchantId(), id, relationId); return ApiResult.success(); }

    @GetMapping("/{id}/rewards")
    @RequirePermission("merchant:referral:view")
    public ApiResult<List<ReferralRewardVO>> rewards(@PathVariable Long id) { return ApiResult.success(referralService.rewards(merchantId(), id)); }

    @PostMapping("/{id}/rewards/{rewardId}/revoke")
    @OpLog(action = "REFERRAL_REWARD_REVOKE", targetType = "REFERRAL_REWARD", targetIdExpr = "#rewardId")
    @RequirePermission("merchant:referral:reward:revoke")
    public ApiResult<Void> revoke(@PathVariable Long id, @PathVariable Long rewardId,
                                  @RequestParam(required = false) String reason) { referralService.revokeReward(merchantId(), id, rewardId, reason); return ApiResult.success(); }

    private Long merchantId() { CurrentUser user = CurrentUserHolder.get(); if (user == null || user.getMerchantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN); return user.getMerchantId(); }
}
