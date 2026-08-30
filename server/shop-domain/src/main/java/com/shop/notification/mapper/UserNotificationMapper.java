package com.shop.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.notification.entity.UserNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    @Insert("INSERT IGNORE INTO user_notification " +
            "(user_id, merchant_id, type, title, content, biz_type, biz_id, link, is_read, created_at, updated_at, deleted) " +
            "VALUES (#{userId}, #{merchantId}, #{type}, #{title}, #{content}, #{bizType}, #{bizId}, #{link}, 0, NOW(), NOW(), 0)")
    int insertIgnore(UserNotification notification);
}
