package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "refund_application", autoResultMap = true)
public class RefundApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    /** 商户退款单号，作为微信退款接口的幂等键。 */
    private String outRefundNo;

    /** 微信退款单号。 */
    private String wxRefundId;

    private Long userId;

    private Long merchantId;

    private String reason;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> evidenceUrls;

    private Integer status;

    private String rejectReason;

    private BigDecimal refundAmount;

    /** 用户申请退款的订单商品与数量 JSON，满减订单据此重算剩余商品优惠。 */
    private String refundItemJson;

    private String refundFailReason;

    private String refundRawPayload;

    private LocalDateTime refundTime;

    /** 是否由系统自动发起退款（例如拼团失败）。 */
    private Integer autoRefund;

    private Integer returnRequired;

    private String returnShipCompany;

    private String returnShipNo;

    private LocalDateTime returnShipTime;

    private LocalDateTime returnReceivedTime;

    private String returnReceiveNote;

    private LocalDateTime refundReconcileAt;

    private Integer refundReconcileAttempts;

    private String refundReconcileError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
