package com.shop.marketing.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class PromotionCartProgressRequest {
    @NotEmpty private List<Long> cartItemIds;
}
