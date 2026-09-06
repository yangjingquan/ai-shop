package com.shop.lottery.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.lottery.dto.*;
import com.shop.lottery.service.LotteryService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wx/lottery")
@RequiredArgsConstructor
public class WxLotteryController {
    private final LotteryService lotteryService;
    private final WxMerchantResolver merchantResolver;

    @GetMapping("/current")
    public ApiResult<LotteryActivityVO> current(HttpServletRequest request) {
        return ApiResult.success(lotteryService.current(merchantResolver.requireActiveMerchant(request), userId()));
    }

    @GetMapping("/{id}")
    public ApiResult<LotteryActivityVO> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(lotteryService.getActive(merchantResolver.requireActiveMerchant(request), userId(), id));
    }

    @PostMapping("/{id}/draw")
    public ApiResult<LotteryDrawVO> draw(@PathVariable Long id, @RequestBody @Valid LotteryDrawRequest body, HttpServletRequest request) {
        return ApiResult.success(lotteryService.draw(merchantResolver.requireActiveMerchant(request), userId(), id, body));
    }

    @GetMapping("/{id}/records")
    public ApiResult<List<LotteryDrawVO>> records(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(lotteryService.records(merchantResolver.requireActiveMerchant(request), userId(), id));
    }

    @GetMapping("/rewards")
    public ApiResult<List<LotteryRewardVO>> rewards(HttpServletRequest request) {
        return ApiResult.success(lotteryService.userRewards(merchantResolver.requireActiveMerchant(request), userId()));
    }

    @PostMapping("/rewards/{rewardId}/address")
    public ApiResult<Void> address(@PathVariable Long rewardId, @RequestBody @Valid LotteryAddressRequest body, HttpServletRequest request) {
        lotteryService.saveAddress(merchantResolver.requireActiveMerchant(request), userId(), rewardId, body.getAddressId());
        return ApiResult.success(null);
    }

    private Long userId() { return CurrentUserHolder.get() == null ? null : CurrentUserHolder.get().getUserId(); }
}
