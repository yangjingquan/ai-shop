package com.shop.presale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("presale_order")
public class PresaleOrder extends BaseEntity {
    private String orderNo;
    private String depositOrderNo;
    private String balanceOrderNo;
    private Long merchantId;
    private Long activityId;
    private Long presaleSkuId;
    private Long productId;
    private Long skuId;
    private Long userId;
    private Integer quantity;
    private Integer stage;
    private String productName;
    private String mainImage;
    private String specText;
    private BigDecimal depositAmount;
    private BigDecimal depositDeductionAmount;
    private BigDecimal balanceAmount;
    private BigDecimal finalPrice;
    private BigDecimal finalAmount;
    private LocalDateTime depositPaidAt;
    private LocalDateTime balanceDeadline;
    private LocalDateTime balancePaidAt;
    private LocalDateTime expectedShipAt;
}
