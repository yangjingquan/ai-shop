package com.shop.points.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointsRedeemRecordVO {

    private Long id;

    private String redeemNo;

    private String title;

    private String image;

    /** COUPON / PHYSICAL */
    private String redeemType;

    private Integer pointsCost;

    private Integer quantity;

    private String orderNo;

    private Long couponId;

    private Integer orderStatus;

    private String statusText;

    private LocalDateTime createdAt;
}
