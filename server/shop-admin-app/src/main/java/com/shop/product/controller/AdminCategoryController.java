package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.product.dto.CategoryRequest;
import com.shop.product.dto.CategoryVO;
import com.shop.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public ApiResult<List<CategoryVO>> tree() {
        return ApiResult.success(categoryService.adminTree());
    }

    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody CategoryRequest req) {
        return ApiResult.success(categoryService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        categoryService.update(id, req);
        return ApiResult.success();
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> setStatus(@PathVariable Long id, @RequestParam int status) {
        categoryService.setStatus(id, status);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResult.success();
    }
}
