package com.shop.user.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.user.dto.UserAddressRequest;
import com.shop.user.dto.UserAddressVO;
import com.shop.user.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/addresses")
@RequiredArgsConstructor
public class WxAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public ApiResult<List<UserAddressVO>> list() {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(userAddressService.list(userId));
    }

    @GetMapping("/{id}")
    public ApiResult<UserAddressVO> get(@PathVariable Long id) {
        Long userId = CurrentUserHolder.get().getUserId();
        return ApiResult.success(userAddressService.get(userId, id));
    }

    @PostMapping
    public ApiResult<Map<String, Long>> create(@RequestBody @Valid UserAddressRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        Long id = userAddressService.create(userId, req);
        return ApiResult.success(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid UserAddressRequest req) {
        Long userId = CurrentUserHolder.get().getUserId();
        userAddressService.update(userId, id, req);
        return ApiResult.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long userId = CurrentUserHolder.get().getUserId();
        userAddressService.delete(userId, id);
        return ApiResult.success(null);
    }

    @PutMapping("/{id}/default")
    public ApiResult<Void> setDefault(@PathVariable Long id) {
        Long userId = CurrentUserHolder.get().getUserId();
        userAddressService.setDefault(userId, id);
        return ApiResult.success(null);
    }
}
