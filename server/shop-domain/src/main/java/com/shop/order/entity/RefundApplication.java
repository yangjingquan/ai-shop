package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_application")
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

    private Integer status;

    private String rejectReason;

    private BigDecimal refundAmount;

    private String refundFailReason;

    private String refundRawPayload;

    private LocalDateTime refundTime;

    /** 是否由系统自动发起退款（例如拼团失败）。 */
    private Integer autoRefund;

    private LocalDateTime refundReconcileAt;

    private Integer refundReconcileAttempts;

    private String refundReconcileError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
