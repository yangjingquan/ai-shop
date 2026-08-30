package com.shop.order.service;

import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;

public interface WxPayService {
    OrderCreateVO.PayParams createJsapiPayParams(Order order);

    Transaction queryOrder(Order order);

    void closeOrder(Order order);

    /** 发起全额原路退款；outRefundNo 必须对同一业务退款保持不变。 */
    Refund createRefund(Order order, String outRefundNo, String reason);

    /** 查询退款单，用于补偿任务或人工重试。 */
    Refund queryRefund(Order order, String outRefundNo);
}
