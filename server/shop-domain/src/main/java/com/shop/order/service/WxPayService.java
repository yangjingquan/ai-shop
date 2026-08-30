package com.shop.order.service;

import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;

public interface WxPayService {
    OrderCreateVO.PayParams createJsapiPayParams(Order order);

    Transaction queryOrder(Order order);

    void closeOrder(Order order);

    /** 兼容旧调用，默认发起全额原路退款。 */
    default Refund createRefund(Order order, String outRefundNo, String reason) {
        return createRefund(order, outRefundNo, reason, order.getPayAmount());
    }

    /** 发起指定金额的原路退款；outRefundNo 必须对同一业务退款保持不变。 */
    Refund createRefund(Order order, String outRefundNo, String reason, java.math.BigDecimal refundAmount);

    /** 查询退款单，用于补偿任务或人工重试。 */
    Refund queryRefund(Order order, String outRefundNo);
}
