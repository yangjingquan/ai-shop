package com.shop.groupbuy.service;

import com.shop.common.response.PageResult;
import com.shop.groupbuy.dto.GroupBuyCreateRequest;
import com.shop.groupbuy.dto.GroupBuyCreateVO;
import com.shop.groupbuy.dto.GroupBuyGroupVO;
import com.shop.groupbuy.dto.GroupBuyProductDetailVO;
import com.shop.product.dto.ProductListVO;

public interface GroupBuyService {
    PageResult<ProductListVO> productPage(int page, int size, Long merchantId, Long categoryId, String keyword);
    GroupBuyProductDetailVO productDetail(Long productId, Long merchantId);
    GroupBuyCreateVO openGroup(Long userId, GroupBuyCreateRequest req);
    GroupBuyCreateVO openGroup(Long userId, Long merchantId, GroupBuyCreateRequest req);
    GroupBuyCreateVO joinGroup(Long userId, Long groupId, GroupBuyCreateRequest req);
    GroupBuyCreateVO joinGroup(Long userId, Long merchantId, Long groupId, GroupBuyCreateRequest req);
    GroupBuyGroupVO groupDetail(Long groupId);
    GroupBuyGroupVO groupDetail(Long groupId, Long merchantId);
    void handleOrderPaid(String orderNo);
    int failExpiredGroups(int batchLimit);
}
