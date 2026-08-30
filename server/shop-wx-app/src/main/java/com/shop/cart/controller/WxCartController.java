package com.shop.cart.controller;

import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.dto.CartBatchDeleteRequest;
import com.shop.cart.dto.CartItemVO;
import com.shop.cart.dto.CartUpdateRequest;
import com.shop.cart.service.CartService;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/cart")
@RequiredArgsConstructor
public class WxCartController {

    private final CartService cartService;
    private final WxMerchantResolver wxMerchantResolver;

    @PostMapping
    public ApiResult<Map<String, Long>> add(@RequestBody @Valid CartAddRequest req, HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        Long id = cartService.add(userId, merchantId, req);
        return ApiResult.success(Map.of("id", id));
    }

    @GetMapping
    public ApiResult<List<CartItemVO>> list(HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        wxMerchantResolver.requireActiveMerchant(request);
        return ApiResult.success(cartService.list(userId));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid CartUpdateRequest req,
                                  HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        cartService.update(userId, merchantId, id, req);
        return ApiResult.success(null);
    }

    @DeleteMapping("/batch")
    public ApiResult<Void> deleteBatch(@RequestBody @Valid CartBatchDeleteRequest req, HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        cartService.deleteBatch(userId, merchantId, req.getIds());
        return ApiResult.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        cartService.delete(userId, merchantId, id);
        return ApiResult.success(null);
    }
}
