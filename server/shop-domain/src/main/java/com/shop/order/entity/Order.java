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

    private String quoteId;

    private Long ruleVersion;

    private Long userId;

    private Long merchantId;

    private Integer status;

    /** 0 普通订单 / 1 团购订单 / 2 秒杀订单 / 3 积分兑换订单 / 4 搭配购订单 / 6 预售支付订单 */
    private Integer orderType;

    /** 0=无需履约，1=快递发货。 */
    private Integer fulfillmentMethod;

    /** 下单时冻结的订单领域快照，避免后续活动/商品编辑改变历史订单语义。 */
    private String orderSnapshotJson;

    /** 积分兑换订单关联记录；仅 orderType=3 使用。 */
    private Long pointsRedeemId;

    private Long groupBuyGroupId;

    /** 2=秒杀订单。 */
    private Long seckillSessionId;

    private Long seckillSkuId;

    /** 4=搭配购订单。 */
    private Long bundleActivityId;

    /** 5=抽奖实物奖励订单。 */
    private Long lotteryRewardId;

    /** 预售中心订单 ID；orderType=6 使用。 */
    private Long presaleOrderId;

    /** 预售支付阶段：1=定金，2=尾款。 */
    private Integer presaleStage;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private BigDecimal bundleDiscountAmount;

    private Long couponId;

    private Long couponTemplateId;

    private BigDecimal couponDiscountAmount;

    private String couponSnapshotJson;

    private Long promotionActivityId;

    private BigDecimal promotionDiscountAmount;

    private String promotionSnapshotJson;

    private String pricingSnapshotJson;

    private String bundleSnapshotJson;

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
