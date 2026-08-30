package com.shop.notification.service;

import com.shop.common.response.PageResult;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.notification.dto.UserNotificationVO;

import java.util.List;

public interface UserNotificationService {
    void notifyGroupFormed(GroupBuyGroup group, List<GroupBuyMember> members);
    void notifyGroupFailed(GroupBuyGroup group, List<GroupBuyMember> members);
    PageResult<UserNotificationVO> page(Long userId, Long merchantId, int page, int size);
    long unreadCount(Long userId, Long merchantId);
    void markRead(Long userId, Long merchantId, Long notificationId);
    void markAllRead(Long userId, Long merchantId);
}
