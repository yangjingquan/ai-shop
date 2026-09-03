package com.shop.groupbuy.dto;

import com.shop.product.dto.ProductDetailVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GroupBuyProductDetailVO {
    private ProductDetailVO product;
    private List<GroupBuyGroupVO> groups = new ArrayList<>();
    private Boolean showActiveGroups = true;
}
