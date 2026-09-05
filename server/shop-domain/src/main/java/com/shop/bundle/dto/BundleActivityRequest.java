package com.shop.bundle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BundleActivityRequest {
    @NotBlank
    private String name;
    @NotNull
    private Long mainProductId;
    @NotEmpty
    private List<Long> itemProductIds;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal discountAmount;
    @NotNull
    private LocalDateTime startAt;
    @NotNull
    private LocalDateTime endAt;
    @NotNull
    private Integer status;
}
