package com.shop.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardTrendVO {
    private String date;
    private Long paidOrderCount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private BigDecimal netAmount;
}
