package com.shop.groupbuy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_buy_group")
public class GroupBuyGroup extends BaseEntity {
    private Long merchantId;
    private Long productId;
    private Long leaderUserId;
    private Integer requiredCount;
    private Integer paidCount;
    private Integer status;
    private LocalDateTime expireAt;
    private LocalDateTime formedAt;
}
