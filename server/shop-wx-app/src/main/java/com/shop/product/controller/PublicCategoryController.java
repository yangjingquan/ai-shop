package com.shop.product.controller;

import com.shop.common.response.ApiResult;
import com.shop.product.dto.CategoryVO;
import com.shop.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public ApiResult<List<CategoryVO>> tree() {
        return ApiResult.success(categoryService.tree());
    }
}
