package com.shop.bundle.dto;

import lombok.Data;

import java.util.List;

@Data
public class BundleCartResult {
    private String bundleGroupId;
    private Long bundleActivityId;
    private List<Long> cartItemIds;
}
