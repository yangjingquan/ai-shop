package com.shop.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    @Size(max = 32)
    private String name;

    /** 0 表示一级 */
    private Long parentId;

    private String icon;

    private Integer sort;
}
