package com.shop.cart.controller;

import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.dto.CartItemVO;
import com.shop.cart.dto.CartUpdateRequest;
import com.shop.cart.service.CartService;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
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

    @PostMapping
    public ApiResult<Map<String, Long>> add(@RequestBody @Valid CartAddRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long id = cartService.add(userId, req);
        return ApiResult.success(Map.of("id", id));
    }

    @GetMapping
    public ApiResult<List<CartItemVO>> list() {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(cartService.list(userId));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid CartUpdateRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        cartService.update(userId, id, req);
        return ApiResult.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long userId = CurrentUserHolder.get().getUserId();
        cartService.delete(userId, id);
        return ApiResult.success(null);
    }
}
