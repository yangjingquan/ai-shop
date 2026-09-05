package com.shop.marketing.service;

import com.shop.marketing.dto.*;
import java.util.List;

public interface PromotionService {
    List<PromotionActivityVO> list(Long merchantId);
    List<PromotionActivityVO> listActive(Long merchantId);
    PromotionActivityVO get(Long merchantId, Long id);
    Long create(Long merchantId, PromotionActivityRequest request);
    void update(Long merchantId, Long id, PromotionActivityRequest request);
    void updateStatus(Long merchantId, Long id, Integer status);
    PromotionCheckoutResult calculate(Long merchantId, List<PromotionPricingItem> items);
    PromotionCheckoutResult calculateActivity(Long activityId, List<PromotionPricingItem> items);
    void reserve(String orderNo, PromotionCheckoutResult result);
    void markPaid(String orderNo);
    void release(String orderNo);
}
