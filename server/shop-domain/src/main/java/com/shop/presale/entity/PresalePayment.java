package com.shop.presale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("presale_payment")
public class PresalePayment extends BaseEntity {
    private Long presaleOrderId;
    private String orderNo;
    private Integer paymentStage;
    private BigDecimal amount;
    private String transactionId;
    private Integer status;
}
