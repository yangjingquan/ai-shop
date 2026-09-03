package com.shop.groupbuy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_buy_notification_log")
public class GroupBuyNotificationLog extends BaseEntity {
    private Long merchantId;
    private Long groupId;
    private Long userId;
    private String eventType;
    private String templateType;
    private String templateId;
    private String status;
    private Integer errcode;
    private String errmsg;
    private LocalDateTime sentAt;
}
