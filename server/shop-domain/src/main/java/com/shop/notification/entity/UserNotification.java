package com.shop.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_notification")
public class UserNotification extends BaseEntity {
    private Long userId;
    private Long merchantId;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private String bizId;
    private String link;
    private Integer isRead;
    private LocalDateTime readAt;
}
