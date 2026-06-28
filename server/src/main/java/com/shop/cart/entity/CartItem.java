package com.shop.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart_item")
public class CartItem extends BaseEntity {

    private Long userId;

    private Long merchantId;

    private Long productId;

    private Long skuId;

    private Integer quantity;
}
