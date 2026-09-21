package com.shop.customer.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CustomerOperationVO {
    private CustomerOperationVO() {}

    @Data
    public static class Tag {
        private Long id;
        private String code;
        private String name;
        private String tagType;
        private String color;
        private Integer status;
        private Long userCount;
    }

    @Data
    public static class Metric {
        private Integer paidOrderCount;
        private BigDecimal totalPaidAmount;
        private LocalDateTime firstPaidAt;
        private LocalDateTime lastPaidAt;
        private Integer memberLevel;
        private Integer pointsBalance;
        private Integer unusedCouponCount;
        private Integer expiringCouponCount;
        private Integer favoriteCount;
        private Integer historyCount;
        private LocalDateTime lastViewedAt;
        private LocalDateTime calculatedAt;
    }

    @Data
    public static class UserSummary {
        private Long userId;
        private String nickname;
        private String avatar;
        private String phone;
        private LocalDateTime joinedAt;
        private LocalDateTime lastLoginAt;
        private Metric metric;
        private List<Tag> tags = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserDetail extends UserSummary {
        private List<String> systemTagNames = new ArrayList<>();
        private LocalDateTime recentOrderAt;
        private LocalDateTime recentFavoriteAt;
    }

    @Data
    public static class Segment {
        private Long id;
        private String name;
        private String description;
        private String segmentType;
        private CustomerOperationRequest.SegmentConditionGroup condition;
        private Integer memberCount;
        private LocalDateTime calculatedAt;
        private Integer status;
        private LocalDateTime updatedAt;
    }
}
