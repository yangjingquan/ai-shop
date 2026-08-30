package com.shop.notification.service.impl;

import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.notification.entity.UserNotification;
import com.shop.notification.mapper.UserNotificationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplTest {

    @Mock
    private UserNotificationMapper notificationMapper;

    @Test
    void sendsFormedMessageOnlyToPaidMembers() {
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        GroupBuyGroup group = group();
        GroupBuyMember paid = member(1L, GroupBuyMemberStatus.PAID);
        GroupBuyMember unpaid = member(2L, GroupBuyMemberStatus.WAIT_PAY);

        service.notifyGroupFormed(group, List.of(paid, unpaid));

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).insertIgnore(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("GROUP_FORMED", captor.getValue().getType());
        assertEquals("/pages/group-buy/group?groupId=9", captor.getValue().getLink());
    }

    @Test
    void explainsAutomaticRefundInFailedMessage() {
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        GroupBuyMember waitingRefund = member(1L, GroupBuyMemberStatus.WAIT_REFUND);

        service.notifyGroupFailed(group(), List.of(waitingRefund));

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).insertIgnore(captor.capture());
        assertEquals("GROUP_FAILED", captor.getValue().getType());
        assertTrue(captor.getValue().getContent().contains("原路退款"));
    }

    private GroupBuyGroup group() {
        GroupBuyGroup group = new GroupBuyGroup();
        group.setId(9L);
        group.setMerchantId(3L);
        return group;
    }

    private GroupBuyMember member(Long userId, GroupBuyMemberStatus status) {
        GroupBuyMember member = new GroupBuyMember();
        member.setUserId(userId);
        member.setStatus(status.getCode());
        return member;
    }
}
