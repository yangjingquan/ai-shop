package com.shop.order.service;

import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface WxPayService {
    OrderCreateVO.PayParams createJsapiPayParams(Order order);

    Transaction queryOrder(Order order);

    void closeOrder(Order order);
}
