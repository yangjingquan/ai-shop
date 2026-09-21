package com.shop.journey.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.shop.common.entity.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("marketing_journey_execution") public class MarketingJourneyExecution extends BaseEntity { private Long merchantId; private Long journeyId; private Long enrollmentId; private Long userId; private Long couponId; private Long notificationId; private String status; private String reason; private LocalDateTime executedAt; }
