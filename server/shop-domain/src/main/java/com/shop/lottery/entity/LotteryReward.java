package com.shop.lottery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lottery_reward")
public class LotteryReward extends BaseEntity {
    private Long drawRecordId;
    private Long activityId;
    private Long merchantId;
    private Long userId;
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private Integer pointsAmount;
    private Long couponTemplateId;
    private Long couponId;
    private String orderNo;
    private String addressSnapshot;
    private Integer status;
    private String failureReason;
    private LocalDateTime issuedAt;
    private LocalDateTime claimedAt;
}
