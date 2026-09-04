package com.shop.seckill.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.seckill.dto.*;
import com.shop.seckill.service.SeckillService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wx/seckill")
@RequiredArgsConstructor
public class WxSeckillController {
    private final SeckillService seckillService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping("/sessions")
    public ApiResult<List<SeckillSessionVO>> sessions(HttpServletRequest request) {
        return ApiResult.success(seckillService.sessions(wxMerchantResolver.currentMerchantId(request)));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResult<SeckillSessionVO> session(@PathVariable Long sessionId, HttpServletRequest request) {
        return ApiResult.success(seckillService.sessionDetail(wxMerchantResolver.currentMerchantId(request), sessionId));
    }

    @GetMapping("/products/{productId}")
    public ApiResult<SeckillProductDetailVO> product(@PathVariable Long productId,
                                                     @RequestParam Long sessionId,
                                                     @RequestParam Long seckillSkuId,
                                                     HttpServletRequest request) {
        return ApiResult.success(seckillService.productDetail(wxMerchantResolver.currentMerchantId(request), productId, sessionId, seckillSkuId));
    }

    @PostMapping("/orders/preview")
    public ApiResult<SeckillOrderPreviewVO> preview(@RequestBody @Valid SeckillOrderPreviewRequest request,
                                                    HttpServletRequest httpRequest) {
        return ApiResult.success(seckillService.preview(CurrentUserHolder.get().getUserId(),
                wxMerchantResolver.requireActiveMerchant(httpRequest), request));
    }

    @PostMapping("/orders")
    public ApiResult<com.shop.order.dto.OrderCreateVO> create(@RequestBody @Valid SeckillOrderCreateRequest request,
                                                              HttpServletRequest httpRequest) {
        return ApiResult.success(seckillService.createOrder(CurrentUserHolder.get().getUserId(),
                wxMerchantResolver.requireActiveMerchant(httpRequest), request));
    }
}
