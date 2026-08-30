package com.shop.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyAmountRow {
    private LocalDate day;
    private Long count;
    private BigDecimal amount;
}
