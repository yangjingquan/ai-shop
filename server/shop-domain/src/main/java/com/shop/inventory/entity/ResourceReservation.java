package com.shop.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_reservation")
public class ResourceReservation extends BaseEntity {
    private String orderNo;
    private Long merchantId;
    private Long productId;
    private Long skuId;
    private String resourceType;
    private String resourceId;
    private Integer quantity;
    /** 0=RESERVED, 1=CONFIRMED, 2=RELEASED, 3=RESTOCKED. */
    private Integer status;
    private String reason;
    private LocalDateTime confirmedAt;
    private LocalDateTime releasedAt;
}
