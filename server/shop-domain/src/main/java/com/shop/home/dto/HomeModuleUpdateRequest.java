package com.shop.home.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class HomeModuleUpdateRequest {
    @NotEmpty
    @Valid
    private List<Item> modules;

    @Data
    public static class Item {
        @NotBlank private String code;
        private String title;
        private String subtitle;
        @Min(0) @Max(1) private Integer enabled;
        @Min(0) @Max(999) private Integer sortOrder;
        private String productSource;
        @Min(1) @Max(20) private Integer productLimit;
    }
}
