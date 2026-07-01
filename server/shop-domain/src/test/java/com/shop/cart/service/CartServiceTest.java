package com.shop.cart.service;

import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.dto.CartItemVO;
import com.shop.cart.dto.CartUpdateRequest;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class CartServiceTest {

    @Autowired
    private CartService cartService;

    private static final Long WX_USER = 3L;

    @Test
    void addThenAddAgainShouldAccumulate() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(2);
        Long cid1 = cartService.add(WX_USER, req);
        assertNotNull(cid1);

        CartAddRequest req2 = new CartAddRequest();
        req2.setSkuId(9L);
        req2.setQuantity(3);
        Long cid2 = cartService.add(WX_USER, req2);
        assertEquals(cid1, cid2); // 同一 sku 去重累加

        List<CartItemVO> items = cartService.list(WX_USER);
        assertEquals(1, items.size());
        assertEquals(5, items.get(0).getQuantity());
    }

    @Test
    void listReturnsAvailableAndUnavailable() {
        List<CartItemVO> items = cartService.list(WX_USER);
        for (CartItemVO item : items) {
            if (item.getAvailable()) {
                assertTrue(item.getUnavailableReason().isEmpty());
            } else {
                assertFalse(item.getUnavailableReason().isEmpty());
            }
        }
    }

    @Test
    void updateToZeroShouldDelete() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        CartUpdateRequest up = new CartUpdateRequest();
        up.setQuantity(0);
        cartService.update(WX_USER, cid, up);

        List<CartItemVO> items = cartService.list(WX_USER);
        assertTrue(items.stream().noneMatch(i -> i.getId().equals(cid)));
    }

    @Test
    void deleteNotOwnedShouldThrow() {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(9L);
        req.setQuantity(1);
        Long cid = cartService.add(WX_USER, req);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.delete(WX_USER + 999, cid));
        assertEquals(ErrorCode.CART_ITEM_NOT_OWNED.getCode(), ex.getCode());
    }
}
