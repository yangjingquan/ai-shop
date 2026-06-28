package com.shop.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wx/order")
@RequiredArgsConstructor
@Profile("!prod")
public class WxOrderMockPayController {

    private final OrderPaymentService orderPaymentService;
    private final OrderMapper orderMapper;

    @PostMapping("/{orderNo}/mock-pay")
    public ApiResult<Void> mockPay(@PathVariable String orderNo) {
        Long userId = CurrentUserHolder.get().getUserId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        String txnId = "MOCK_TXN_" + UUID.randomUUID().toString().replace("-", "");
        String payload = "{\"mock\":true,\"orderNo\":\"" + orderNo + "\"}";
        orderPaymentService.handlePaidCallback(orderNo, txnId, payload);
        return ApiResult.success(null);
    }
}
