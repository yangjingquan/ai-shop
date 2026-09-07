package com.shop.presale.service;

import com.shop.common.response.PageResult;
import com.shop.order.dto.OrderCreateVO;
import com.shop.pricing.dto.QuoteResult;
import com.shop.presale.dto.*;

import java.util.List;

public interface PresaleService {
    List<PresaleActivityVO> active(Long merchantId);
    PresaleActivityVO detail(Long merchantId, Long activityId);
    PresaleOrderVO order(Long userId, Long merchantId, String orderNo);
    QuoteResult quoteDeposit(Long userId, Long merchantId, PresaleDepositQuoteRequest request);
    OrderCreateVO createDepositOrder(Long userId, Long merchantId, PresaleDepositOrderRequest request);
    OrderCreateVO createBalanceOrder(Long userId, Long merchantId, String orderNo);
    void refundDeposit(Long userId, Long merchantId, String orderNo);
    void cancelOrRefundDeposit(Long userId, Long merchantId, String orderNo);
    void updateAddress(Long userId, Long merchantId, String orderNo, Long addressId);
    void handleOrderPaid(String orderNo, String transactionId, String rawPayload);
    void syncShipping(Long merchantId, String balanceOrderNo);
    void syncFinished(Long userId, String orderNo);
    String afterSalesOrderNo(Long userId, String orderNo);
    void assertPayable(Long userId, Long merchantId, String orderNo);
    int expireBalanceOrders(int limit);
    PageResult<PresaleActivityVO> merchantPage(Long merchantId, int page, int size);
    PresaleActivityVO merchantGet(Long merchantId, Long activityId);
    Long saveActivity(Long merchantId, Long operatorId, PresaleActivitySaveRequest request);
    void updateActivity(Long merchantId, Long operatorId, Long activityId, PresaleActivitySaveRequest request);
    PresaleStatsVO stats(Long merchantId, Long activityId);
}
