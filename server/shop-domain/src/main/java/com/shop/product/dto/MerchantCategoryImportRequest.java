package com.shop.product.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MerchantCategoryImportRequest {

    @NotEmpty
    private List<Long> sourceCategoryIds;

    private Boolean includeChildren;
}
