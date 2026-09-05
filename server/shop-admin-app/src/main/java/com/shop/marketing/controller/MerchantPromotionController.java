package com.shop.marketing.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.marketing.dto.PromotionActivityRequest;
import com.shop.marketing.dto.PromotionActivityVO;
import com.shop.marketing.service.PromotionService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchant/promotions")
@RequiredArgsConstructor
public class MerchantPromotionController {
    private final PromotionService promotionService;
    @GetMapping @RequirePermission("merchant:promotion:view")
    public ApiResult<List<PromotionActivityVO>> list() { return ApiResult.success(promotionService.list(CurrentUserHolder.get().getMerchantId())); }
    @GetMapping("/{id}") @RequirePermission("merchant:promotion:view")
    public ApiResult<PromotionActivityVO> get(@PathVariable Long id) { return ApiResult.success(promotionService.get(CurrentUserHolder.get().getMerchantId(), id)); }
    @PostMapping @RequirePermission("merchant:promotion:manage")
    public ApiResult<Long> create(@RequestBody @Valid PromotionActivityRequest request) { return ApiResult.success(promotionService.create(CurrentUserHolder.get().getMerchantId(), request)); }
    @PutMapping("/{id}") @RequirePermission("merchant:promotion:manage")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid PromotionActivityRequest request) { promotionService.update(CurrentUserHolder.get().getMerchantId(), id, request); return ApiResult.success(null); }
    @PutMapping("/{id}/status") @RequirePermission("merchant:promotion:manage")
    public ApiResult<Void> status(@PathVariable Long id, @RequestBody @Valid StatusRequest request) { promotionService.updateStatus(CurrentUserHolder.get().getMerchantId(), id, request.status); return ApiResult.success(null); }
    @Data public static class StatusRequest { private Integer status; }
}
