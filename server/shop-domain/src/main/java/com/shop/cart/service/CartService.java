package com.shop.cart.service;

import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.dto.CartItemVO;
import com.shop.cart.dto.CartUpdateRequest;

import java.util.List;

public interface CartService {

    /** 加购：同 (userId, skuId) 已有未删则数量累加；否则新增。返回 cart_item.id */
    Long add(Long userId, CartAddRequest req);

    /** 商家上下文内加购；merchantId 为 null 时仅供内部兼容调用。 */
    Long add(Long userId, Long merchantId, CartAddRequest req);

    /** 列出当前用户购物车，附带 merchant/product 快照与可用性判断 */
    List<CartItemVO> list(Long userId);

    /** 修改数量；quantity=0 视为删除 */
    void update(Long userId, Long cartItemId, CartUpdateRequest req);

    /** 商家上下文内修改数量。 */
    void update(Long userId, Long merchantId, Long cartItemId, CartUpdateRequest req);

    /** 删除单条 */
    void delete(Long userId, Long cartItemId);

    /** 商家上下文内删除。 */
    void delete(Long userId, Long merchantId, Long cartItemId);

    /** 批量删除当前用户购物车项 */
    void deleteBatch(Long userId, List<Long> cartItemIds);

    /** 商家上下文内批量删除。 */
    void deleteBatch(Long userId, Long merchantId, List<Long> cartItemIds);
}
