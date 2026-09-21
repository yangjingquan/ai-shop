package com.shop.journey.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import lombok.Data;
@Data public class MarketingJourneyRequest { @NotBlank private String name; @NotBlank private String triggerType; private Long segmentId; @NotNull private Integer delayMinutes; private Long couponTemplateId; private String notificationTitle; private String notificationContent; @NotNull private Integer frequencyDays; @NotNull private Integer stopOnPaid; @NotNull private Integer status; }
