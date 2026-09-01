package com.shop.order.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LogisticsTrackingVO {

    private String orderNo;
    private String shipCompany;
    private String shipperCode;
    private String shipNo;
    private String state;
    private String stateText;
    private LocalDateTime lastTime;
    private String lastContent;
    private LocalDateTime syncedAt;
    private String error;
    private List<Trace> traces;

    @Data
    public static class Trace {
        private LocalDateTime acceptTime;
        private String acceptStation;
        private String state;
        private String stateText;
    }
}
