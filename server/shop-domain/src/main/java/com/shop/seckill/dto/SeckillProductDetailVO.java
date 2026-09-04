package com.shop.seckill.dto;

import com.shop.product.dto.ProductDetailVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SeckillProductDetailVO {
    private Long sessionId;
    private String sessionName;
    private Long activityId;
    private String activityName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer sessionStatus;
    private String sessionStatusText;
    private Long seckillSkuId;
    private Long productId;
    private String productName;
    private String subtitle;
    private String mainImage;
    private List<String> images = new ArrayList<>();
    private String description;
    private Long categoryId;
    private Integer totalSales;
    private BigDecimal activityPrice;
    private BigDecimal originalPrice;
    private Integer activityStock;
    private Integer soldCount;
    private Integer remainingStock;
    private Integer userLimit;
    private Long skuId;
    private String specText;
    private BigDecimal skuPrice;
    private List<ProductDetailVO.SpecVO> specs = new ArrayList<>();
    private List<SeckillSkuOptionVO> skus = new ArrayList<>();

    @Data
    public static class SeckillSkuOptionVO {
        private Long seckillSkuId;
        private Long skuId;
        private String specText;
        private BigDecimal activityPrice;
        private BigDecimal originalPrice;
        private List<Long> specValueIds = new ArrayList<>();
        private String image;
        private Integer stock;
        private Integer activityStock;
        private Integer soldCount;
        private Integer remainingStock;
        private Integer userLimit;
    }
}
