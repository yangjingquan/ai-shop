package com.shop.dashboard.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A time-bounded, auditable merchant settlement view. Net operating income is intentionally separate from payable settlement. */
@Data
public class SettlementAnalysisVO {
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private String timezone;
    private LocalDateTime dataAsOf;
    private BigDecimal createdGmv = BigDecimal.ZERO;
    private Long createdOrderCount = 0L;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private Long paidOrderCount = 0L;
    private BigDecimal successfulRefundAmount = BigDecimal.ZERO;
    private BigDecimal processingRefundAmount = BigDecimal.ZERO;
    private BigDecimal operatingNetAmount = BigDecimal.ZERO;
    private BigDecimal platformCommission = BigDecimal.ZERO;
    private BigDecimal channelFee = BigDecimal.ZERO;
    private BigDecimal subsidyAdjustment = BigDecimal.ZERO;
    private BigDecimal provisionalSettlementAmount = BigDecimal.ZERO;
    private String settlementStatus;
    private String settlementNotice;
    private String metricDefinition;
}
