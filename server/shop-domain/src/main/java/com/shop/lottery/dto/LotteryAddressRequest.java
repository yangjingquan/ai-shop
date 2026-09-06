package com.shop.lottery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LotteryAddressRequest {
    @NotNull private Long addressId;
}
