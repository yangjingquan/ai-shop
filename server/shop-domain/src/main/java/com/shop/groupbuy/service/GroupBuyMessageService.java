package com.shop.groupbuy.service;

import com.shop.groupbuy.dto.GroupBuySubscribeRequest;
import com.shop.groupbuy.dto.GroupBuySubscriptionConfigVO;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;

import java.util.List;

public interface GroupBuyMessageService {
    GroupBuySubscriptionConfigVO subscriptionConfig(Long merchantId, Long groupId);
    void recordSubscriptions(Long userId, Long merchantId, GroupBuySubscribeRequest request);
    void recordShare(Long userId, Long merchantId, Long groupId, String source, boolean opened);
    void notifyGroupFormed(GroupBuyGroup group, List<GroupBuyMember> members);
    void notifyGroupFailed(GroupBuyGroup group, List<GroupBuyMember> members);
    int notifyExpiring(int batchLimit);
}
