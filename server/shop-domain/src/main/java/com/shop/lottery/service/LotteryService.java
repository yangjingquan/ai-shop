package com.shop.lottery.service;

import com.shop.lottery.dto.*;

import java.util.List;

public interface LotteryService {
    LotteryActivityVO current(Long merchantId, Long userId);
    LotteryActivityVO getActive(Long merchantId, Long userId, Long activityId);
    LotteryDrawVO draw(Long merchantId, Long userId, Long activityId, LotteryDrawRequest request);
    List<LotteryDrawVO> records(Long merchantId, Long userId, Long activityId);
    List<LotteryRewardVO> userRewards(Long merchantId, Long userId);
    void saveAddress(Long merchantId, Long userId, Long rewardId, Long addressId);

    List<LotteryActivityVO> merchantList(Long merchantId);
    LotteryActivityVO merchantGet(Long merchantId, Long activityId);
    Long create(Long merchantId, LotteryActivitySaveRequest request);
    void update(Long merchantId, Long activityId, LotteryActivitySaveRequest request);
    void updateStatus(Long merchantId, Long activityId, Integer status);
    LotteryStatsVO stats(Long merchantId, Long activityId);
    List<LotteryRewardVO> merchantRewards(Long merchantId, Long activityId);
    void updateRewardStatus(Long merchantId, Long activityId, Long rewardId, Integer status);
}
