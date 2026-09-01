package com.shop.order.service;

import com.shop.order.dto.LogisticsTrackingVO;

public interface LogisticsService {

    LogisticsTrackingVO trackForUser(Long userId, String orderNo, boolean forceRefresh);

    LogisticsTrackingVO trackForMerchant(Long merchantId, String orderNo, boolean forceRefresh);

    LogisticsTrackingVO trackForAdmin(String orderNo, boolean forceRefresh);
}
