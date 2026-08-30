package com.shop.home.service.impl;

import com.shop.banner.service.BannerService;
import com.shop.common.response.PageResult;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock
    private BannerService bannerService;
    @Mock
    private ProductService productService;

    @Test
    void loadsBannersWithinCurrentMerchantScope() {
        Long merchantId = 7L;
        when(bannerService.listActive(merchantId)).thenReturn(List.of());
        when(productService.publicPage(1, 10, merchantId, null, null, null, null))
                .thenReturn(PageResult.of(List.of(), 0, 1, 10));
        when(productService.publicTopSalesPage(1, 10, merchantId, null))
                .thenReturn(PageResult.<ProductListVO>of(List.of(), 0, 1, 10));

        assertTrue(new HomeServiceImpl(bannerService, productService).getHome(merchantId).getBanners().isEmpty());
        verify(bannerService).listActive(merchantId);
    }
}
