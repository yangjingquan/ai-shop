package com.shop.groupbuy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupBuyShareEventRequest {
    @NotNull
    private Long groupId;
    private String source;
    private Boolean opened;
}
