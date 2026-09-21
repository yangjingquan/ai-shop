package com.shop.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_segment_member")
public class CustomerSegmentMember extends BaseEntity {
    private Long merchantId;
    private Long segmentId;
    private Long userId;
    private LocalDateTime snapshotAt;
}
