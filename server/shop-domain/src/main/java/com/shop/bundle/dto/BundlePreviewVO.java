package com.shop.bundle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BundlePreviewVO {
    private Long bundleId;
    private String bundleName;
    private BigDecimal originalAmount;
    private BigDecimal bundleDiscountAmount;
    private BigDecimal payAmount;
    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private Long skuId;
        private String productName;
        private String mainImage;
        private String specText;
        private BigDecimal unitPrice;
        private Integer quantity;
        private Integer stock;
        private Boolean available;
        private String unavailableReason;
    }
}
