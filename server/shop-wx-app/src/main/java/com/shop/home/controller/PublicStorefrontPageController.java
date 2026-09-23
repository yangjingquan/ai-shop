package com.shop.home.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.home.dto.StorefrontPageVO;
import com.shop.home.service.StorefrontPageService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/store-pages")
@RequiredArgsConstructor
public class PublicStorefrontPageController {
    private final StorefrontPageService storefrontPageService;
    private final WxMerchantResolver merchantResolver;

    @GetMapping("/{slug}")
    public ApiResult<StorefrontPageVO> topic(@PathVariable String slug, HttpServletRequest request) {
        StorefrontPageVO page = storefrontPageService.publicTopic(merchantResolver.currentMerchantId(request), slug);
        if (page == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "专题不存在或已下线");
        return ApiResult.success(page);
    }
}
