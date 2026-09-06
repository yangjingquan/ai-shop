package com.shop.lottery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LotteryDrawRequest {
    @NotBlank private String idempotencyKey;
}
