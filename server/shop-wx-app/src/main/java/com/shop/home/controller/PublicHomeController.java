package com.shop.home.controller;

import com.shop.common.response.ApiResult;
import com.shop.home.dto.HomeVO;
import com.shop.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicHomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public ApiResult<HomeVO> getHome() {
        return ApiResult.success(homeService.getHome());
    }
}
