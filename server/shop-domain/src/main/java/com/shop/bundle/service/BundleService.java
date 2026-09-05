package com.shop.bundle.service;

import com.shop.bundle.dto.*;
import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.dto.OrderPreviewRequest;
import com.shop.order.dto.OrderPreviewVO;

import java.util.List;

public interface BundleService {
    List<BundleActivityVO> merchantList(Long merchantId);
    BundleActivityVO merchantGet(Long merchantId, Long id);
    Long save(Long merchantId, Long operatorId, Long id, BundleActivityRequest request);
    void delete(Long merchantId, Long id);

    BundleActivityVO findActiveForProduct(Long merchantId, Long productId);
    BundleActivityVO publicGet(Long merchantId, Long id);
    BundlePreviewVO preview(Long merchantId, BundlePreviewRequest request);
    BundleCartResult addToCart(Long userId, Long merchantId, BundleCartRequest request);
    boolean containsBundleCartItems(Long userId, Long merchantId, List<Long> cartItemIds);
    OrderPreviewVO previewOrder(Long userId, Long merchantId, OrderPreviewRequest request);
    List<OrderCreateVO> createOrder(Long userId, Long merchantId, OrderCreateRequest request);
}
