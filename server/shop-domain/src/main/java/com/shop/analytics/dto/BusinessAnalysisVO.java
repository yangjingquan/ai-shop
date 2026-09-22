package com.shop.analytics.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data public class BusinessAnalysisVO {
    private LocalDateTime from; private LocalDateTime to; private LocalDateTime dataAsOf; private String metricDefinition;
    private Overview overview; private Funnel funnel; private List<SourceRow> sources; private List<MerchantRow> merchantPerformance;
    @Data public static class Overview { private long paidOrderCount; private long paidUserCount; private BigDecimal paidAmount; private BigDecimal refundAmount; private BigDecimal netAmount; private BigDecimal actualDiscountAmount; private BigDecimal averageOrderValue; private BigDecimal refundRate; private BigDecimal attributionCoverage; private long unmatchedPaymentCount; }
    @Data public static class Funnel { private long productViewUsers; private long addCartUsers; private long submitOrderUsers; private long paidUsers; private BigDecimal addCartRate; private BigDecimal submitRate; private BigDecimal payRate; private String coverageNote; }
    @Data public static class SourceRow { private String source; private String sourceName; private long paidOrderCount; private BigDecimal paidAmount; private BigDecimal refundAmount; private BigDecimal netAmount; private BigDecimal actualDiscountAmount; private BigDecimal coverage; }
    @Data public static class MerchantRow { private Long merchantId; private String merchantName; private long paidOrderCount; private BigDecimal paidAmount; private BigDecimal refundAmount; private BigDecimal netAmount; }
}
