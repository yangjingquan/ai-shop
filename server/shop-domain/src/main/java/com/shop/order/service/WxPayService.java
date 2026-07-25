package com.shop.order.service;

import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;

public interface WxPayService {
    OrderCreateVO.PayParams createJsapiPayParams(Order order);
}
