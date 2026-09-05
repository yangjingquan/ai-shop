package com.shop.referral.controller;

import com.shop.common.aop.RateLimit;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.referral.dto.ReferralBindRequest;
import com.shop.referral.dto.ReferralCampaignVO;
import com.shop.referral.dto.ReferralRewardVO;
import com.shop.referral.service.ReferralService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/referrals")
@RequiredArgsConstructor
public class WxReferralController {
    private final ReferralService referralService;
    private final WxMerchantResolver merchantResolver;

    @GetMapping("/current")
    public ApiResult<ReferralCampaignVO> current(@RequestParam(required = false) String token, HttpServletRequest request) {
        return ApiResult.success(referralService.current(merchantResolver.requireActiveMerchant(request), userId(), token));
    }

    @GetMapping("/{campaignId}")
    public ApiResult<ReferralCampaignVO> get(@PathVariable Long campaignId,
                                             @RequestParam(required = false) String token,
                                             HttpServletRequest request) {
        return ApiResult.success(referralService.get(merchantResolver.requireActiveMerchant(request), userId(), campaignId, token));
    }

    @PostMapping("/{campaignId}/share")
    @RateLimit(key = "referral_share", limit = 20, windowSec = 60, by = RateLimit.By.USER)
    public ApiResult<Map<String, String>> share(@PathVariable Long campaignId, HttpServletRequest request) {
        Long merchantId = merchantResolver.requireActiveMerchant(request);
        return ApiResult.success(Map.of("token", referralService.share(merchantId, userId(), campaignId)));
    }

    @PostMapping("/{campaignId}/bind")
    @RateLimit(key = "referral_bind", limit = 5, windowSec = 60, by = RateLimit.By.USER)
    public ApiResult<ReferralCampaignVO> bind(@PathVariable Long campaignId, @RequestBody ReferralBindRequest body,
                                              HttpServletRequest request) {
        return ApiResult.success(referralService.bind(merchantResolver.requireActiveMerchant(request), userId(), campaignId, body.getToken()));
    }

    @PostMapping("/{campaignId}/claim")
    @RateLimit(key = "referral_claim", limit = 3, windowSec = 60, by = RateLimit.By.USER)
    public ApiResult<Long> claim(@PathVariable Long campaignId, HttpServletRequest request) {
        return ApiResult.success(referralService.claimInviteeCoupon(merchantResolver.requireActiveMerchant(request), userId(), campaignId));
    }

    @GetMapping("/{campaignId}/rewards")
    public ApiResult<List<ReferralRewardVO>> rewards(@PathVariable Long campaignId, HttpServletRequest request) {
        return ApiResult.success(referralService.userRewards(merchantResolver.requireActiveMerchant(request), userId(), campaignId));
    }

    private Long userId() { return CurrentUserHolder.get().getUserId(); }
}
