package com.shop.presale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresaleActivitySaveRequest {
    @NotBlank private String activityName;
    private String description;
    private String bannerImage;
    @NotNull private LocalDateTime depositStartAt;
    @NotNull private LocalDateTime depositEndAt;
    @NotNull private LocalDateTime balanceStartAt;
    @NotNull private LocalDateTime balanceEndAt;
    @NotNull private LocalDateTime expectedShipAt;
    private Integer autoCloseExpired = 1;
    private String depositRefundRule;
    private String merchantBreachRule;
    @Valid @NotEmpty private List<PresaleSkuSaveRequest> skus = new ArrayList<>();
}
