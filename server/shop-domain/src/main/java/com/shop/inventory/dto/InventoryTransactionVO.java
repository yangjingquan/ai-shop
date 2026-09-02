package com.shop.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryTransactionVO {
    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String skuCode;
    private String specText;
    private Integer changeQty;
    private Integer stockBefore;
    private Integer stockAfter;
    private String operationType;
    private String referenceNo;
    private String reason;
    private Long operatorId;
    private LocalDateTime createdAt;
}
