package com.shop.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data @EqualsAndHashCode(callSuper = true) @TableName("order_attribution")
public class OrderAttribution extends BaseEntity {
    private Long merchantId; private String orderNo; private String primarySource; private Long sourceId; private String sourceName; private String evidenceType; private String assistSource; private LocalDateTime attributedAt;
}
