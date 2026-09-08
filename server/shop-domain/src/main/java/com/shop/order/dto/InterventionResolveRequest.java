package com.shop.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterventionResolveRequest {
    @NotBlank private String resolution;
}
