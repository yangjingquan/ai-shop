package com.shop.inventory.service.impl;

import com.shop.common.exception.BusinessException;
import com.shop.inventory.dto.InventoryAdjustmentRequest;
import com.shop.inventory.entity.InventoryTransaction;
import com.shop.inventory.mapper.InventoryTransactionMapper;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private ProductSkuMapper skuMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private InventoryTransactionMapper transactionMapper;
    @Mock
    private ProductService productService;

    @Test
    void adjustsStockAndCreatesLedger() {
        ProductSku sku = new ProductSku();
        sku.setId(10L);
        sku.setProductId(20L);
        sku.setStock(5);
        sku.setActive(1);
        Product product = new Product();
        product.setId(20L);
        product.setMerchantId(7L);
        when(skuMapper.selectOne(any())).thenReturn(sku);
        when(productMapper.selectById(20L)).thenReturn(product);
        when(skuMapper.adjustStock(10L, 5, 8)).thenReturn(1);

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setSkuId(10L);
        request.setChangeQty(3);
        request.setReason("盘点入库");

        new InventoryServiceImpl(skuMapper, productMapper, transactionMapper, productService)
                .adjust(7L, 99L, request);

        ArgumentCaptor<InventoryTransaction> transaction = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionMapper).insert(transaction.capture());
        assertEquals(3, transaction.getValue().getChangeQty());
        assertEquals(5, transaction.getValue().getStockBefore());
        assertEquals(8, transaction.getValue().getStockAfter());
        assertEquals("盘点入库", transaction.getValue().getReason());
        verify(productService).recalcProduct(20L);
    }

    @Test
    void rejectsZeroAdjustment() {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setSkuId(10L);
        request.setChangeQty(0);

        InventoryServiceImpl service = new InventoryServiceImpl(
                skuMapper, productMapper, transactionMapper, productService);

        assertThrows(BusinessException.class, () -> service.adjust(7L, 99L, request));
    }

    @Test
    void rejectsAdjustmentThatWouldMakeStockNegative() {
        ProductSku sku = new ProductSku();
        sku.setId(10L);
        sku.setProductId(20L);
        sku.setStock(5);
        sku.setActive(1);
        Product product = new Product();
        product.setId(20L);
        product.setMerchantId(7L);
        when(skuMapper.selectOne(any())).thenReturn(sku);
        when(productMapper.selectById(20L)).thenReturn(product);

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setSkuId(10L);
        request.setChangeQty(-6);

        InventoryServiceImpl service = new InventoryServiceImpl(
                skuMapper, productMapper, transactionMapper, productService);

        assertThrows(BusinessException.class, () -> service.adjust(7L, 99L, request));
        verify(skuMapper, never()).adjustStock(any(), any(Integer.class), any(Integer.class));
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void rejectsSkuOwnedByAnotherMerchant() {
        ProductSku sku = new ProductSku();
        sku.setId(10L);
        sku.setProductId(20L);
        sku.setStock(5);
        sku.setActive(1);
        Product product = new Product();
        product.setId(20L);
        product.setMerchantId(8L);
        when(skuMapper.selectOne(any())).thenReturn(sku);
        when(productMapper.selectById(20L)).thenReturn(product);

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setSkuId(10L);
        request.setChangeQty(3);

        InventoryServiceImpl service = new InventoryServiceImpl(
                skuMapper, productMapper, transactionMapper, productService);

        assertThrows(BusinessException.class, () -> service.adjust(7L, 99L, request));
        verify(skuMapper, never()).adjustStock(any(), any(Integer.class), any(Integer.class));
        verify(transactionMapper, never()).insert(any());
    }
}
