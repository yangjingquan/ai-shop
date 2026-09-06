package com.shop.presale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PresaleOrderVO {
    private String orderNo;
    private String depositOrderNo;
    private String balanceOrderNo;
    private Long activityId;
    private String activityName;
    private Long presaleSkuId;
    private String productName;
    private String mainImage;
    private String specText;
    private Integer quantity;
    private Integer stage;
    private String stageText;
    private BigDecimal depositAmount;
    private BigDecimal depositDeductionAmount;
    private BigDecimal balanceAmount;
    private BigDecimal finalPrice;
    private BigDecimal finalAmount;
    private LocalDateTime depositPaidAt;
    private LocalDateTime balanceDeadline;
    private LocalDateTime balancePaidAt;
    private LocalDateTime expectedShipAt;
    private boolean canPayBalance;
    private boolean canRefundDeposit;
    private boolean canChangeAddress;
}
