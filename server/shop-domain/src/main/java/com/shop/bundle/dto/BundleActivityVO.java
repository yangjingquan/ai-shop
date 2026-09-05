package com.shop.bundle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BundleActivityVO {
    private Long id;
    private String name;
    private Long mainProductId;
    private String mainProductName;
    private String mainProductImage;
    private List<BundleSkuVO> mainSkus;
    private BigDecimal discountAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    private String statusText;
    private Boolean active;
    private List<BundleItemVO> items;
}
