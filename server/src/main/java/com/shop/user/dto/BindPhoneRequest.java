package com.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindPhoneRequest {
    @NotBlank
    private String code;
}
