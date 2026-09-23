package com.shop.home.dto;

import com.shop.product.dto.ProductListVO;
import lombok.Data;
import java.util.List;

@Data
public class HomeModuleVO {
    private String id;
    private String code;
    private String name;
    private String description;
    private String title;
    private String subtitle;
    private Integer enabled;
    private Integer sortOrder;
    private String productSource;
    private Integer productLimit;
    private List<ProductListVO> products;
    private List<StorefrontPageSummaryVO> topicPages;
}
