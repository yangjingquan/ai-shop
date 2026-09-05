package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private Integer status;

    /** 0 普通订单 / 1 团购订单 / 2 秒杀订单 / 3 积分兑换订单 */
    private Integer orderType;

    /** 积分兑换订单关联记录；仅 orderType=3 使用。 */
    private Long pointsRedeemId;

    private Long groupBuyGroupId;

    /** 2=秒杀订单。 */
    private Long seckillSessionId;

    private Long seckillSkuId;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private Long couponId;

    private Long couponTemplateId;

    private BigDecimal couponDiscountAmount;

    private String couponSnapshotJson;

    private Long promotionActivityId;

    private BigDecimal promotionDiscountAmount;

    private String promotionSnapshotJson;

    private BigDecimal payAmount;

    private Integer payMethod;

    private LocalDateTime payTime;

    private String payTransactionId;

    private LocalDateTime payReconcileAt;

    private Integer payReconcileAttempts;

    private String payReconcileError;

    /** JSON string，service 层序列化 AddressSnapshot */
    private String addressSnapshot;

    private String shipNo;

    private String shipCompany;

    private String shipperCode;

    private String logisticsState;

    private String logisticsStateText;

    private LocalDateTime logisticsLastTime;

    private String logisticsLastContent;

    private LocalDateTime logisticsSyncedAt;

    private String logisticsError;

    private LocalDateTime shipTime;

    private LocalDateTime shipReminderAt;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    /** 用户侧隐藏标记；不影响商家后台及订单相关业务。 */
    private Integer userDeleted;
}
