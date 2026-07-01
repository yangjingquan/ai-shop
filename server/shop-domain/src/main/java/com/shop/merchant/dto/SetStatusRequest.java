package com.shop.merchant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetStatusRequest {
    @NotNull
    private Integer status; // 1 启用 / 0 冻结
}
