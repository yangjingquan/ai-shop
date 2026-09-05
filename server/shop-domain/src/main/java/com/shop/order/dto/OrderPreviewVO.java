package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import com.shop.marketing.dto.PromotionCheckoutResult;

@Data
public class OrderPreviewVO {

    private List<MerchantGroup> groups;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private Long couponId;

    private String couponName;

    private BigDecimal couponDiscountAmount;

    private String couponMessage;

    private Long promotionActivityId;

    private String promotionName;

    private BigDecimal promotionDiscountAmount;

    private PromotionCheckoutResult promotion;

    private List<com.shop.coupon.dto.CouponVO> coupons;

    private AddressSnapshot address;

    @Data
    public static class MerchantGroup {

        private Long merchantId;

        private String merchantName;

        private List<PreviewItem> items;

        private BigDecimal totalAmount;

        private BigDecimal freightAmount;

        private BigDecimal discountAmount;

        private BigDecimal payAmount;
    }

    @Data
    public static class PreviewItem {

        private Long cartItemId;

        private Long skuId;

        private Long productId;

        private String productName;

        private String mainImage;

        private String specText;

        private Integer quantity;

        private BigDecimal unitPrice;

        private BigDecimal subtotal;

        private boolean available;

        private String unavailableReason;
    }
}
