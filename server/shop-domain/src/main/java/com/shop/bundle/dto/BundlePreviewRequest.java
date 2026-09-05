package com.shop.bundle.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BundlePreviewRequest {
    @NotNull
    private Long bundleId;
    @NotNull
    private Long mainSkuId;
    private List<Long> itemSkuIds;
}
