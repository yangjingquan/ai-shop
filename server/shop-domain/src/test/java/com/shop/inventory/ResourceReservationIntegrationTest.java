package com.shop.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.inventory.entity.ResourceReservation;
import com.shop.inventory.enums.ReservationStatus;
import com.shop.inventory.mapper.ResourceReservationMapper;
import com.shop.inventory.service.ResourceReservationService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class ResourceReservationIntegrationTest {
    @Autowired private ResourceReservationService resourceReservationService;
    @Autowired private ResourceReservationMapper reservationMapper;
    @Autowired private ProductSkuMapper skuMapper;
    @Autowired private ProductMapper productMapper;

    @Test
    void reserveReleaseAndRepeatedReleaseKeepStockAndStateConsistent() {
        ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getDeleted, 0).gt(ProductSku::getStock, 0).last("LIMIT 1"));
        assertNotNull(sku);
        Product product = productMapper.selectById(sku.getProductId());
        assertNotNull(product);
        String orderNo = "I003-RESERVE-" + System.nanoTime();
        int stockBefore = sku.getStock();

        resourceReservationService.reserveSku(orderNo, product.getMerchantId(), product.getId(), sku.getId(), 1, "集成测试");
        assertEquals(stockBefore - 1, skuMapper.selectById(sku.getId()).getStock());
        ResourceReservation reserved = reservationMapper.selectOne(new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getOrderNo, orderNo));
        assertEquals(ReservationStatus.RESERVED.getCode(), reserved.getStatus());

        resourceReservationService.releaseOrder(orderNo, "集成测试取消");
        resourceReservationService.releaseOrder(orderNo, "重复取消");
        assertEquals(stockBefore, skuMapper.selectById(sku.getId()).getStock());
        assertEquals(ReservationStatus.RELEASED.getCode(), reservationMapper.selectById(reserved.getId()).getStatus());
    }
}
