package com.shop.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDetailVO {

    private Long id;
    private Long merchantId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String subtitle;
    private String mainImage;
    private List<String> images;
    private String description;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private Integer totalSales;
    private Integer status;
    private Integer sort;

    private List<SpecVO> specs = new ArrayList<>();
    private List<SkuVO> skus = new ArrayList<>();

    @Data
    public static class SpecVO {
        private Long id;
        private String name;
        private Integer sort;
        private List<SpecValueVO> values = new ArrayList<>();
    }

    @Data
    public static class SpecValueVO {
        private Long id;
        private String value;
        private Integer sort;
    }

    @Data
    public static class SkuVO {
        private Long id;
        private String skuCode;
        private List<Long> specValueIds;
        private String specText;
        private BigDecimal price;
        private Integer stock;
        private String image;
    }
}
