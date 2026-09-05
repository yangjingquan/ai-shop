package com.shop.referral.service;

import com.shop.order.entity.Order;
import com.shop.referral.dto.*;

import java.util.List;

public interface ReferralService {
    ReferralCampaignVO current(Long merchantId, Long userId, String token);
    ReferralCampaignVO get(Long merchantId, Long userId, Long campaignId, String token);
    String share(Long merchantId, Long userId, Long campaignId);
    ReferralCampaignVO bind(Long merchantId, Long userId, Long campaignId, String token);
    Long claimInviteeCoupon(Long merchantId, Long userId, Long campaignId);
    List<ReferralRewardVO> userRewards(Long merchantId, Long userId, Long campaignId);

    List<ReferralCampaignVO> merchantList(Long merchantId);
    ReferralCampaignVO merchantGet(Long merchantId, Long campaignId);
    Long create(Long merchantId, ReferralCampaignSaveRequest request);
    void update(Long merchantId, Long campaignId, ReferralCampaignSaveRequest request);
    void updateStatus(Long merchantId, Long campaignId, Integer status);
    ReferralStatsVO stats(Long merchantId, Long campaignId);
    List<ReferralRelationVO> relations(Long merchantId, Long campaignId);
    List<ReferralRewardVO> rewards(Long merchantId, Long campaignId);
    void freezeRelation(Long merchantId, Long campaignId, Long relationId);
    void revokeReward(Long merchantId, Long campaignId, Long rewardId, String reason);

    void handleOrderPaid(Order order);
    void handleOrderRefunded(String orderNo);
}
