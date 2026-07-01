package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListVO {

    private String orderNo;

    private Integer status;

    private String statusText;

    private BigDecimal payAmount;

    private Long merchantId;

    private String merchantName;

    private Integer itemCount;

    private String itemSummary;

    private String firstItemImage;

    private LocalDateTime createdAt;

    /** 毫秒时间戳，仅 WAIT_PAY 状态时有值 */
    private Long expireAt;
}
