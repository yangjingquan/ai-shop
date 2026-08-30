package com.shop.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNotificationVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private String bizId;
    private String link;
    private Integer isRead;
    private LocalDateTime createdAt;
}
