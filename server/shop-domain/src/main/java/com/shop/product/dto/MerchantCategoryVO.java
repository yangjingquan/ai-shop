package com.shop.product.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MerchantCategoryVO {

    private Long id;

    private Long merchantId;

    private Long sourceCategoryId;

    private Long parentId;

    private String name;

    private String icon;

    private Integer level;

    private Integer sort;

    private Integer status;

    private List<MerchantCategoryVO> children = new ArrayList<>();
}
