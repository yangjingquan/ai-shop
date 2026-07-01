package com.shop.cart.service;

import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.dto.CartItemVO;
import com.shop.cart.dto.CartUpdateRequest;

import java.util.List;

public interface CartService {

    /** 加购：同 (userId, skuId) 已有未删则数量累加；否则新增。返回 cart_item.id */
    Long add(Long userId, CartAddRequest req);

    /** 列出当前用户购物车，附带 merchant/product 快照与可用性判断 */
    List<CartItemVO> list(Long userId);

    /** 修改数量；quantity=0 视为删除 */
    void update(Long userId, Long cartItemId, CartUpdateRequest req);

    /** 删除单条 */
    void delete(Long userId, Long cartItemId);
}
