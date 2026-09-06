package com.shop.presale.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PresaleStatsVO {
    private Long depositOrderCount;
    private Long balancePaidOrderCount;
    private Long overdueOrderCount;
    private Long refundOrderCount;
    private BigDecimal finalAmount = BigDecimal.ZERO;
}
