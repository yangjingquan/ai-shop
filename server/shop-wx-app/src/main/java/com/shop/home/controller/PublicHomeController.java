package com.shop.home.controller;

import com.shop.common.response.ApiResult;
import com.shop.home.dto.HomeVO;
import com.shop.home.service.HomeService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicHomeController {

    private final HomeService homeService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping("/home")
    public ApiResult<HomeVO> getHome(HttpServletRequest request) {
        return ApiResult.success(homeService.getHome(wxMerchantResolver.currentMerchantId(request)));
    }
}
