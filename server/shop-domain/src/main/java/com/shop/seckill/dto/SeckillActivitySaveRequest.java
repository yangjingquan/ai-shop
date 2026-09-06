package com.shop.seckill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SeckillActivitySaveRequest {
    @NotEmpty
    @Size(max = 128)
    private String activityName;
    @Size(max = 500)
    private String description;
    private LocalDateTime preheatAt;
    @NotEmpty
    @Valid
    private List<Session> sessions = new ArrayList<>();

    @Data
    public static class Session {
        private Long id;
        @NotEmpty
        @Size(max = 64)
        private String name;
        @NotNull
        private LocalDateTime startAt;
        @NotNull
        private LocalDateTime endAt;
        private Integer sort = 0;
        @NotEmpty
        @Valid
        private List<Sku> skus = new ArrayList<>();
    }

    @Data
    public static class Sku {
        private Long id;
        @NotNull
        private Long productId;
        @NotNull
        private Long skuId;
        @NotNull
        private BigDecimal activityPrice;
        @NotNull
        private Integer activityStock;
        @NotNull
        private Integer userLimit = 1;
    }
}
