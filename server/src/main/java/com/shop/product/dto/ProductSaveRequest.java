package com.shop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSaveRequest {

    @NotBlank
    private String name;

    private String subtitle;

    @NotNull
    private Long categoryId;

    private String mainImage;

    private List<String> images;

    private String description;

    @NotEmpty
    @Valid
    private List<SpecInput> specs;

    @NotEmpty
    @Valid
    private List<SkuInput> skus;

    @Data
    public static class SpecInput {
        @NotBlank
        private String name;

        @NotEmpty
        private List<String> values;
    }

    @Data
    public static class SkuInput {
        /** 每个规格在 specs[i].values 数组里的下标 */
        @NotEmpty
        private List<Integer> specValueIndexes;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal price;

        @NotNull
        @Min(0)
        private Integer stock;

        private String skuCode;

        private String image;
    }
}
