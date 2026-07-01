package com.shop.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {

    private Long id;

    private Long merchantId;

    private String merchantName;

    private Long productId;

    private String productName;

    private String mainImage;

    private Integer productStatus;

    private Long skuId;

    private String specText;

    private BigDecimal unitPrice;

    /** 当前 SKU 库存（用于前端校验） */
    private Integer stock;

    private Integer quantity;

    /** 行小计 = unitPrice * quantity */
    private BigDecimal subtotal;

    /** 是否可下单（库存>=quantity 且商品上架且 SKU 未删） */
    private Boolean available;

    /** 不可下单原因：OFF_SHELF / SKU_GONE / STOCK_NOT_ENOUGH */
    private String unavailableReason;
}
