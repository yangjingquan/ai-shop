package com.shop.analytics.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class AnalyticsRequest {
    @Data public static class ProductView { @NotBlank private String eventId; @NotNull private Long productId; private String channelCode; }
}
