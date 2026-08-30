package com.shop.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.response.PageResult;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.notification.dto.UserNotificationVO;
import com.shop.notification.entity.UserNotification;
import com.shop.notification.mapper.UserNotificationMapper;
import com.shop.notification.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final String BIZ_TYPE_GROUP_BUY = "GROUP_BUY";
    private static final String TYPE_GROUP_FORMED = "GROUP_FORMED";
    private static final String TYPE_GROUP_FAILED = "GROUP_FAILED";

    private final UserNotificationMapper notificationMapper;

    @Override
    public void notifyGroupFormed(GroupBuyGroup group, List<GroupBuyMember> members) {
        for (GroupBuyMember member : members) {
            if (member.getStatus() == GroupBuyMemberStatus.PAID.getCode()) {
                insertGroupMessage(group, member, TYPE_GROUP_FORMED, "拼团成功",
                        "你的拼团已成功，订单已进入待发货状态。");
            }
        }
    }

    @Override
    public void notifyGroupFailed(GroupBuyGroup group, List<GroupBuyMember> members) {
        for (GroupBuyMember member : members) {
            String content = member.getStatus() == GroupBuyMemberStatus.WAIT_REFUND.getCode()
                    ? "拼团未在有效期内成团，已为你的已付款订单发起原路退款。"
                    : "拼团未在有效期内成团，你的未付款订单已自动取消。";
            insertGroupMessage(group, member, TYPE_GROUP_FAILED, "拼团失败", content);
        }
    }

    @Override
    public PageResult<UserNotificationVO> page(Long userId, Long merchantId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        IPage<UserNotification> result = notificationMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getMerchantId, merchantId)
                        .orderByAsc(UserNotification::getIsRead)
                        .orderByDesc(UserNotification::getCreatedAt));
        List<UserNotificationVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), safePage, safeSize);
    }

    @Override
    public long unreadCount(Long userId, Long merchantId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getMerchantId, merchantId)
                .eq(UserNotification::getIsRead, 0));
    }

    @Override
    public void markRead(Long userId, Long merchantId, Long notificationId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getMerchantId, merchantId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadAt, LocalDateTime.now()));
    }

    @Override
    public void markAllRead(Long userId, Long merchantId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getMerchantId, merchantId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadAt, LocalDateTime.now()));
    }

    private void insertGroupMessage(GroupBuyGroup group, GroupBuyMember member,
                                    String type, String title, String content) {
        UserNotification notification = new UserNotification();
        notification.setUserId(member.getUserId());
        notification.setMerchantId(group.getMerchantId());
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizType(BIZ_TYPE_GROUP_BUY);
        notification.setBizId(String.valueOf(group.getId()));
        notification.setLink("/pages/group-buy/group?groupId=" + group.getId());
        notificationMapper.insertIgnore(notification);
    }

    private UserNotificationVO toVO(UserNotification notification) {
        UserNotificationVO vo = new UserNotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setBizType(notification.getBizType());
        vo.setBizId(notification.getBizId());
        vo.setLink(notification.getLink());
        vo.setIsRead(notification.getIsRead());
        vo.setCreatedAt(notification.getCreatedAt());
        return vo;
    }
}
