package com.shop.engagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.response.PageResult;
import com.shop.engagement.dto.EngagementVO;
import com.shop.engagement.entity.UserProductFavorite;
import com.shop.engagement.entity.UserProductHistory;
import com.shop.engagement.mapper.ProductQuestionMapper;
import com.shop.engagement.mapper.ProductReviewMapper;
import com.shop.engagement.mapper.UserProductFavoriteMapper;
import com.shop.engagement.mapper.UserProductHistoryMapper;
import com.shop.engagement.service.impl.ProductEngagementServiceImpl;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductEngagementServiceImplTest {

    @Mock private ProductReviewMapper reviewMapper;
    @Mock private ProductQuestionMapper questionMapper;
    @Mock private UserProductFavoriteMapper favoriteMapper;
    @Mock private UserProductHistoryMapper historyMapper;
    @Mock private ProductMapper productMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private ProductEngagementServiceImpl service;

    @Test
    void favoritesKeepCollectionOrderAndExposeUnavailableProductsForRemoval() {
        UserProductFavorite newest = favorite(20L, 101L);
        UserProductFavorite older = favorite(10L, 102L);
        Page<UserProductFavorite> source = new Page<>(1, 10);
        source.setRecords(List.of(newest, older));
        source.setTotal(2);
        when(favoriteMapper.selectPage(any(), ArgumentMatchers.any())).thenReturn(source);
        when(productMapper.selectBatchIds(List.of(101L, 102L))).thenReturn(List.of(product(101L, 1, 1), product(102L, 0, 1)));

        PageResult<EngagementVO.ProductRecord> result = service.favorites(20L, 10L, 1, 10);

        assertEquals(2, result.getTotal());
        assertEquals(List.of(101L, 102L), result.getList().stream().map(EngagementVO.ProductRecord::getProductId).toList());
        assertTrue(result.getList().get(0).isAvailable());
        assertFalse(result.getList().get(1).isAvailable());
        verify(productMapper, times(1)).selectBatchIds(List.of(101L, 102L));
    }

    @Test
    void historyUsesLastViewedOrderAndCarriesViewCount() {
        UserProductHistory recent = history(200L, 3);
        Page<UserProductHistory> source = new Page<>(1, 10);
        source.setRecords(List.of(recent));
        source.setTotal(1);
        when(historyMapper.selectPage(any(), ArgumentMatchers.any())).thenReturn(source);
        when(productMapper.selectBatchIds(List.of(200L))).thenReturn(List.of(product(200L, 1, 1)));

        PageResult<EngagementVO.ProductRecord> result = service.histories(20L, 10L, 1, 10);

        assertEquals(1, result.getList().size());
        assertEquals(3, result.getList().get(0).getViewCount());
        assertNotNull(result.getList().get(0).getLastViewedAt());
    }

    @Test
    void canRemoveFavoriteOfDeletedProductWithoutLoadingTheProduct() {
        UserProductFavorite favorite = favorite(88L, 999L);
        favorite.setId(9L);
        when(favoriteMapper.selectOne(ArgumentMatchers.any())).thenReturn(favorite);

        service.favorite(88L, 10L, 999L, false);

        verify(favoriteMapper).deleteById(9L);
        verify(productMapper, never()).selectById(anyLong());
    }

    private UserProductFavorite favorite(Long userId, Long productId) {
        UserProductFavorite value = new UserProductFavorite();
        value.setUserId(userId);
        value.setMerchantId(10L);
        value.setProductId(productId);
        value.setCreatedAt(LocalDateTime.now());
        return value;
    }

    private UserProductHistory history(Long productId, int viewCount) {
        UserProductHistory value = new UserProductHistory();
        value.setUserId(20L);
        value.setMerchantId(10L);
        value.setProductId(productId);
        value.setViewCount(viewCount);
        value.setLastViewedAt(LocalDateTime.now());
        return value;
    }

    private Product product(Long id, int status, int auditStatus) {
        Product value = new Product();
        value.setId(id);
        value.setMerchantId(10L);
        value.setName("测试商品" + id);
        value.setMinPrice(new BigDecimal("19.90"));
        value.setStatus(status);
        value.setAuditStatus(auditStatus);
        return value;
    }
}
