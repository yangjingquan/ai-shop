package com.shop.presale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("presale_activity")
public class PresaleActivity extends BaseEntity {
    private Long merchantId;
    private String name;
    private String description;
    private String bannerImage;
    private LocalDateTime depositStartAt;
    private LocalDateTime depositEndAt;
    private LocalDateTime balanceStartAt;
    private LocalDateTime balanceEndAt;
    private LocalDateTime expectedShipAt;
    private Integer status;
    private Integer autoCloseExpired;
    private String depositRefundRule;
    private String merchantBreachRule;
    private Long createdBy;
}
