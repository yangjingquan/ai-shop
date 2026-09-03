package com.shop.groupbuy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_buy_share_event")
public class GroupBuyShareEvent extends BaseEntity {
    private Long merchantId;
    private Long groupId;
    private Long sharerUserId;
    private Long openerUserId;
    private String source;
    private LocalDateTime openedAt;
}
