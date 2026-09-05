package com.shop.points.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class PointsLedgerVO { private Long id; private Integer changeValue; private Integer balanceAfter; private String source; private String description; private String businessNo; private LocalDateTime createdAt; }
