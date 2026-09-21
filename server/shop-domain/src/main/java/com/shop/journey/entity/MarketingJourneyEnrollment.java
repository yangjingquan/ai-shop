package com.shop.journey.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.shop.common.entity.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("marketing_journey_enrollment") public class MarketingJourneyEnrollment extends BaseEntity { private Long merchantId; private Long journeyId; private Long userId; private String triggerKey; private LocalDateTime triggerAt; private LocalDateTime executeAt; private String status; private String skipReason; }
