package com.shop.bundle.dto;

import lombok.Data;

import java.util.List;

@Data
public class BundleItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private String mainImage;
    private Integer required;
    private Integer sort;
    private List<BundleSkuVO> skus;
}
