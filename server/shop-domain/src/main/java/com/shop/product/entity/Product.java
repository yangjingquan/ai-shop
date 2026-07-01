package com.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product", autoResultMap = true)
public class Product extends BaseEntity {

    private Long merchantId;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String mainImage;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String description;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer totalStock;

    private Integer totalSales;

    /** 1 上架 / 0 下架 */
    private Integer status;

    private Integer sort;
}
