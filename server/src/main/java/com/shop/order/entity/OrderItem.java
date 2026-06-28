package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long productId;

    private Long skuId;

    private String productName;

    private String mainImage;

    private String specText;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    private LocalDateTime createdAt;
}
