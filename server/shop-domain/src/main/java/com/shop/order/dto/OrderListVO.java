package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderListVO {

    private String orderNo;

    private Integer status;

    private String statusText;

    private BigDecimal payAmount;

    private Long merchantId;

    private String merchantName;

    private Integer itemCount;

    private String itemSummary;

    private String firstItemImage;

    private List<OrderItemVO> items;

    private LocalDateTime createdAt;

    /** 毫秒时间戳，仅 WAIT_PAY 状态时有值 */
    private Long expireAt;

    @Data
    public static class OrderItemVO {

        private Long productId;

        private Long skuId;

        private String productName;

        private String mainImage;

        private String specText;

        private BigDecimal unitPrice;

        private Integer quantity;

        private BigDecimal subtotal;
    }
}
