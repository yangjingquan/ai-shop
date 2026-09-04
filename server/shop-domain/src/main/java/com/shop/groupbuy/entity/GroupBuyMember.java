package com.shop.groupbuy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_buy_member")
public class GroupBuyMember extends BaseEntity {
    private Long groupId;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private Integer status;
    private LocalDateTime paidAt;
}
