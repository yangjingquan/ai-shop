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
@TableName(value = "product_sku", autoResultMap = true)
public class ProductSku extends BaseEntity {

    private Long productId;

    private String skuCode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> specValueIds;

    private String specText;

    private BigDecimal price;

    private Integer stock;

    private String image;
}
