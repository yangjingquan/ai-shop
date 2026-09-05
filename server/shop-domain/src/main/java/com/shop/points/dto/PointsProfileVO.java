package com.shop.points.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class PointsProfileVO { private Integer balance; private LocalDateTime joinedAt; private Integer redeemableCouponCount; private boolean memberDayActive; private Integer memberDay; private Integer payAmountYuan; private Integer pointsPerYuan; }
