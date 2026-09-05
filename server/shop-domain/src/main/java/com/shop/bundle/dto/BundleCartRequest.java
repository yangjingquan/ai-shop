package com.shop.bundle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BundleCartRequest {
    @NotNull
    private Long bundleId;
    @NotNull
    private Long mainSkuId;
    @Valid
    private List<Long> itemSkuIds;
}
