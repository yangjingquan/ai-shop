package com.shop.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SeckillAdminSessionVO {
    private Long id;
    private String name;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer sort;
    private List<Sku> skus = new ArrayList<>();

    @Data
    public static class Sku {
        private Long productId;
        private Long skuId;
        private BigDecimal activityPrice;
        private Integer activityStock;
        private Integer soldCount;
        private Integer userLimit;
        private String productName;
        private String specText;
    }
}
