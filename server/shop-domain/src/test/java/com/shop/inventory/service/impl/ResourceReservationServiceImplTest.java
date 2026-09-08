package com.shop.inventory.service.impl;

import com.shop.inventory.entity.InventoryTransaction;
import com.shop.inventory.entity.ResourceReservation;
import com.shop.inventory.enums.ReservationStatus;
import com.shop.inventory.mapper.InventoryTransactionMapper;
import com.shop.inventory.mapper.ResourceReservationMapper;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceReservationServiceImplTest {
    @Mock private ResourceReservationMapper reservationMapper;
    @Mock private ProductSkuMapper skuMapper;
    @Mock private InventoryTransactionMapper transactionMapper;
    @Mock private ProductService productService;

    @Test
    void reservesSkuOnceAndWritesOrderLinkedLedger() {
        ProductSku sku = sku(11L, 21L, 8);
        when(reservationMapper.selectOne(any())).thenReturn(null);
        when(skuMapper.selectOne(any())).thenReturn(sku);
        when(skuMapper.deductStock(11L, 3)).thenReturn(1);

        service().reserveSku("O-1", 7L, 21L, 11L, 3, "待支付");

        ArgumentCaptor<ResourceReservation> reservation = ArgumentCaptor.forClass(ResourceReservation.class);
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(reservationMapper).insert(reservation.capture());
        verify(transactionMapper).insert(tx.capture());
        assertEquals(ReservationStatus.RESERVED.getCode(), reservation.getValue().getStatus());
        assertEquals("O-1", reservation.getValue().getOrderNo());
        assertEquals(-3, tx.getValue().getChangeQty());
        assertEquals("ORDER_RESERVE", tx.getValue().getOperationType());
        assertEquals("O-1", tx.getValue().getReferenceNo());
    }

    @Test
    void repeatedReleaseRestoresSkuOnlyOnce() {
        ResourceReservation reservation = reservation(ReservationStatus.RESERVED.getCode());
        ProductSku sku = sku(11L, 21L, 5);
        when(reservationMapper.selectList(any())).thenReturn(List.of(reservation));
        when(skuMapper.selectOne(any())).thenReturn(sku);

        service().releaseOrder("O-1", "TIMEOUT");
        service().releaseOrder("O-1", "TIMEOUT");

        verify(skuMapper, times(1)).updateById(any(ProductSku.class));
        verify(transactionMapper, times(1)).insert(any(InventoryTransaction.class));
        verify(reservationMapper, times(1)).updateById(reservation);
        assertEquals(ReservationStatus.RELEASED.getCode(), reservation.getStatus());
    }

    @Test
    void repeatedConfirmationDoesNotCreateDuplicateTransition() {
        ResourceReservation reservation = reservation(ReservationStatus.RESERVED.getCode());
        when(reservationMapper.selectList(any())).thenReturn(List.of(reservation));
        when(skuMapper.selectById(11L)).thenReturn(sku(11L, 21L, 5));

        service().confirmOrder("O-1");
        service().confirmOrder("O-1");

        verify(reservationMapper, times(1)).updateById(reservation);
        verify(transactionMapper, times(1)).insert(any(InventoryTransaction.class));
        assertEquals(ReservationStatus.CONFIRMED.getCode(), reservation.getStatus());
    }

    @Test
    void repeatedSkuReservationWithTheSameOrderAndQuantityDoesNotDeductAgain() {
        ResourceReservation existing = reservation(ReservationStatus.RESERVED.getCode());
        when(reservationMapper.selectOne(any())).thenReturn(existing);

        service().reserveSku("O-1", 7L, 21L, 11L, 3, "网络重试");

        verify(skuMapper, times(0)).deductStock(11L, 3);
        verify(reservationMapper, times(0)).insert(any(ResourceReservation.class));
        verify(transactionMapper, times(0)).insert(any(InventoryTransaction.class));
    }

    @Test
    void repeatedEntitlementMarkerDoesNotCreateDuplicateReservation() {
        ResourceReservation existing = reservation(ReservationStatus.RESERVED.getCode());
        when(reservationMapper.selectOne(any())).thenReturn(existing);

        service().reserveMarker("O-1", 7L, "COUPON", "88", 1, "使用优惠券");

        verify(reservationMapper, times(0)).insert(any(ResourceReservation.class));
    }

    private ResourceReservationServiceImpl service() {
        return new ResourceReservationServiceImpl(reservationMapper, skuMapper, transactionMapper, productService);
    }

    private ResourceReservation reservation(int status) {
        ResourceReservation value = new ResourceReservation();
        value.setOrderNo("O-1"); value.setMerchantId(7L); value.setProductId(21L); value.setSkuId(11L);
        value.setResourceType("SKU"); value.setResourceId("11"); value.setQuantity(3); value.setStatus(status);
        return value;
    }

    private ProductSku sku(Long id, Long productId, int stock) {
        ProductSku value = new ProductSku(); value.setId(id); value.setProductId(productId); value.setStock(stock); value.setActive(1);
        return value;
    }
}
