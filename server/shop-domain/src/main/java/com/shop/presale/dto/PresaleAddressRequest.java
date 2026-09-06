package com.shop.presale.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PresaleAddressRequest {
    @NotNull
    private Long addressId;
}
