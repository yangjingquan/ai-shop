package com.shop.product.service;

import com.shop.common.response.PageResult;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.dto.ProductSaveRequest;

public interface ProductService {

    Long create(ProductSaveRequest req, Long merchantId);

    void update(Long id, ProductSaveRequest req, Long merchantId);

    /** merchantId == null 表示公共/admin 视角，不做归属校验 */
    ProductDetailVO get(Long id, Long merchantId);

    PageResult<ProductListVO> page(int page, int size, Long merchantId, Long categoryId, String keyword, Integer status);

    void setStatus(Long id, int status, Long merchantId);

    void delete(Long id, Long merchantId);

    /**
     * 重算 product 的 min_price / max_price / total_stock。
     * M4a 下单/取消/支付回调累计销量后需要调用此方法刷新。
     */
    void recalcProduct(Long productId);
}
