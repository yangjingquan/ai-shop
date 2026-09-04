package com.shop.seckill.service;

import com.shop.common.response.PageResult;
import com.shop.order.dto.OrderCreateVO;
import com.shop.seckill.dto.*;

import java.util.List;

public interface SeckillService {
    List<SeckillSessionVO> sessions(Long merchantId);

    SeckillSessionVO sessionDetail(Long merchantId, Long sessionId);

    SeckillProductDetailVO productDetail(Long merchantId, Long productId, Long sessionId, Long seckillSkuId);

    SeckillOrderPreviewVO preview(Long userId, Long merchantId, SeckillOrderPreviewRequest request);

    OrderCreateVO createOrder(Long userId, Long merchantId, SeckillOrderCreateRequest request);

    void handleOrderPaid(String orderNo);

    void releaseForOrder(String orderNo, String reason);

    PageResult<SeckillActivityVO> merchantPage(Long merchantId, int page, int size);

    SeckillActivityVO merchantGet(Long merchantId, Long activityId);

    Long saveActivity(Long merchantId, Long operatorId, SeckillActivitySaveRequest request);

    void updateActivity(Long merchantId, Long operatorId, Long activityId, SeckillActivitySaveRequest request);
}
