package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {

    private String orderNo;

    private Integer status;

    private String statusText;

    private Integer orderType;

    private Long groupBuyGroupId;

    private Integer groupBuyRequiredCount;

    private Integer groupBuyPaidCount;

    private Long groupBuyExpireAt;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private Long couponId;

    private Long couponTemplateId;

    private BigDecimal couponDiscountAmount;

    private String couponName;

    private BigDecimal payAmount;

    private Long merchantId;

    private String merchantName;

    private AddressSnapshot address;

    private LocalDateTime createdAt;

    private LocalDateTime payTime;

    private String payTransactionId;

    private String shipNo;

    private String shipCompany;

    private String shipperCode;

    private LocalDateTime shipTime;

    private LocalDateTime shipReminderAt;

    private String merchantContactPhone;

    private Integer refundStatus;

    private String refundReason;

    private String refundRejectReason;

    private BigDecimal refundAmount;

    private String refundFailReason;

    private Long refundId;

    private List<String> refundEvidenceUrls;

    private Integer refundReturnRequired;

    private String refundReturnShipCompany;

    private String refundReturnShipNo;

    private LocalDateTime refundReturnShipTime;

    private LocalDateTime refundReturnReceivedTime;

    private String refundReturnReceiveNote;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    private List<OrderItemVO> items;

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
