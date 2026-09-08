package com.shop.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterventionRequest {
    @NotBlank private String reason;
    private String evidenceNote;
}
