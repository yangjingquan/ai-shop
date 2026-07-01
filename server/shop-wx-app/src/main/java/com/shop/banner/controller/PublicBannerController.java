package com.shop.banner.controller;

import com.shop.banner.dto.BannerVO;
import com.shop.banner.service.BannerService;
import com.shop.common.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/banner")
@RequiredArgsConstructor
public class PublicBannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    public ApiResult<List<BannerVO>> list() {
        return ApiResult.success(bannerService.listActive());
    }
}
