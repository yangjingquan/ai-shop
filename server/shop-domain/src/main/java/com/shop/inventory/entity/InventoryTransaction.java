package com.shop.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_transaction")
public class InventoryTransaction extends BaseEntity {

    private Long merchantId;
    private Long productId;
    private Long skuId;
    private Integer changeQty;
    private Integer stockBefore;
    private Integer stockAfter;
    private String operationType;
    private String referenceNo;
    private String reason;
    private Long operatorId;
}
