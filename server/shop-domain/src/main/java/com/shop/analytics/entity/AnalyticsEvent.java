package com.shop.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data @EqualsAndHashCode(callSuper = true) @TableName("analytics_event")
public class AnalyticsEvent extends BaseEntity {
    private Long merchantId; private Long userId; private String eventId; private String eventType; private Long productId; private String channelCode; private LocalDateTime occurredAt;
}
