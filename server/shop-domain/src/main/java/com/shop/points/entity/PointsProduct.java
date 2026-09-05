package com.shop.points.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("points_product")
public class PointsProduct extends BaseEntity { private Long merchantId; private Long productId; private Long skuId; private Long couponTemplateId; private String title; private String image; private Integer pointsPrice; private Integer stock; private Integer perUserLimit; private LocalDateTime validFrom; private LocalDateTime validTo; private Integer status; }
