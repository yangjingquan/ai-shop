package com.shop.banner.controller;

import com.shop.banner.dto.BannerSaveRequest;
import com.shop.banner.dto.BannerVO;
import com.shop.banner.service.BannerService;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
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
    public ApiResult<Map<String, Long>> create(@RequestBody @Valid BannerSaveRequest req) {
        Long id = bannerService.create(req);
        return ApiResult.success(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid BannerSaveRequest req) {
        bannerService.update(id, req);
        return ApiResult.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ApiResult.success(null);
    }

    @GetMapping("/page")
    public ApiResult<PageResult<BannerVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(bannerService.page(page, size));
    }
}
