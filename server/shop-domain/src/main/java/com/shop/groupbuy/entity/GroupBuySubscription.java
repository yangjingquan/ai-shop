package com.shop.groupbuy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_buy_subscription")
public class GroupBuySubscription extends BaseEntity {
    private Long merchantId;
    private Long groupId;
    private Long userId;
    private String templateType;
    private String templateId;
    private String status;
    private LocalDateTime subscribedAt;
    private LocalDateTime sentAt;
    private String sendResult;
}
