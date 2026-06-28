package com.shop.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    @NotEmpty
    private List<Long> cartItemIds;

    @NotNull
    private Long addressId;

    private String remark;
}
