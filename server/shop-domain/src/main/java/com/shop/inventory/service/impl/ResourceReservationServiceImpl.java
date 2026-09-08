package com.shop.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.inventory.entity.InventoryTransaction;
import com.shop.inventory.entity.ResourceReservation;
import com.shop.inventory.enums.ReservationStatus;
import com.shop.inventory.mapper.InventoryTransactionMapper;
import com.shop.inventory.mapper.ResourceReservationMapper;
import com.shop.inventory.service.ResourceReservationService;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceReservationServiceImpl implements ResourceReservationService {
    public static final String SKU = "SKU";

    private final ResourceReservationMapper reservationMapper;
    private final ProductSkuMapper skuMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final ProductService productService;

    @Override
    @Transactional
    public void reserveSku(String orderNo, Long merchantId, Long productId, Long skuId, int quantity, String reason) {
        if (quantity <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预占数量必须大于 0");
        ResourceReservation existing = findLocked(orderNo, SKU, String.valueOf(skuId));
        if (existing != null) {
            if (!Integer.valueOf(quantity).equals(existing.getQuantity())) {
                throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "同一订单的库存预占数量不一致");
            }
            return;
        }
        ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId).eq(ProductSku::getDeleted, 0).last("FOR UPDATE"));
        if (sku == null || skuMapper.deductStock(skuId, quantity) != 1) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        ResourceReservation reservation = reservation(orderNo, merchantId, productId, skuId, SKU,
                String.valueOf(skuId), quantity, reason);
        reservationMapper.insert(reservation);
        recordStock(reservation, -quantity, safe(sku.getStock()), safe(sku.getStock()) - quantity, "ORDER_RESERVE", reason);
        productService.recalcProduct(productId);
    }

    @Override
    @Transactional
    public void reserveMarker(String orderNo, Long merchantId, String resourceType, String resourceId, int quantity, String reason) {
        ResourceReservation existing = findLocked(orderNo, resourceType, resourceId);
        if (existing != null) return;
        reservationMapper.insert(reservation(orderNo, merchantId, null, null, resourceType, resourceId, quantity, reason));
    }

    @Override
    @Transactional
    public void confirmOrder(String orderNo) {
        for (ResourceReservation reservation : reservationsLocked(orderNo)) {
            if (reservation.getStatus() != ReservationStatus.RESERVED.getCode()) continue;
            reservation.setStatus(ReservationStatus.CONFIRMED.getCode());
            reservation.setConfirmedAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
            if (SKU.equals(reservation.getResourceType())) {
                ProductSku sku = skuMapper.selectById(reservation.getSkuId());
                recordStock(reservation, 0, safe(sku == null ? null : sku.getStock()), safe(sku == null ? null : sku.getStock()), "ORDER_CONFIRM", "支付确认");
            }
        }
    }

    @Override
    @Transactional
    public void releaseOrder(String orderNo, String reason) {
        for (ResourceReservation reservation : reservationsLocked(orderNo)) {
            if (reservation.getStatus() != ReservationStatus.RESERVED.getCode()) continue;
            releaseSkuIfPresent(reservation, reason, "ORDER_RELEASE");
            reservation.setStatus(ReservationStatus.RELEASED.getCode());
            reservation.setReason(reason == null ? "" : reason);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
        }
    }

    @Override
    @Transactional
    public void restockConfirmedOrder(String orderNo, String reason) {
        for (ResourceReservation reservation : reservationsLocked(orderNo)) {
            if (reservation.getStatus() != ReservationStatus.CONFIRMED.getCode()) continue;
            releaseSkuIfPresent(reservation, reason, "ORDER_REFUND_RESTOCK");
            reservation.setStatus(ReservationStatus.RESTOCKED.getCode());
            reservation.setReason(reason == null ? "" : reason);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
        }
    }

    private void releaseSkuIfPresent(ResourceReservation reservation, String reason, String operation) {
        if (!SKU.equals(reservation.getResourceType())) return;
        ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getId, reservation.getSkuId()).last("FOR UPDATE"));
        if (sku == null) return;
        int before = safe(sku.getStock());
        skuMapper.updateById(stockAfter(sku, before + reservation.getQuantity()));
        recordStock(reservation, reservation.getQuantity(), before, before + reservation.getQuantity(), operation, reason);
        if (reservation.getProductId() != null) productService.recalcProduct(reservation.getProductId());
    }

    private ProductSku stockAfter(ProductSku sku, int stock) { sku.setStock(stock); return sku; }

    private ResourceReservation findLocked(String orderNo, String type, String resourceId) {
        return reservationMapper.selectOne(new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getOrderNo, orderNo).eq(ResourceReservation::getResourceType, type)
                .eq(ResourceReservation::getResourceId, resourceId).last("FOR UPDATE"));
    }

    private List<ResourceReservation> reservationsLocked(String orderNo) {
        return reservationMapper.selectList(new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getOrderNo, orderNo).last("FOR UPDATE"));
    }

    private ResourceReservation reservation(String orderNo, Long merchantId, Long productId, Long skuId,
                                            String type, String resourceId, int quantity, String reason) {
        ResourceReservation value = new ResourceReservation();
        value.setOrderNo(orderNo); value.setMerchantId(merchantId); value.setProductId(productId); value.setSkuId(skuId);
        value.setResourceType(type); value.setResourceId(resourceId); value.setQuantity(quantity);
        value.setStatus(ReservationStatus.RESERVED.getCode()); value.setReason(reason == null ? "" : reason);
        return value;
    }

    private void recordStock(ResourceReservation reservation, int change, int before, int after, String operation, String reason) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setMerchantId(reservation.getMerchantId()); tx.setProductId(reservation.getProductId()); tx.setSkuId(reservation.getSkuId());
        tx.setChangeQty(change); tx.setStockBefore(before); tx.setStockAfter(after); tx.setOperationType(operation);
        tx.setReferenceNo(reservation.getOrderNo()); tx.setReason(reason == null ? "" : reason);
        transactionMapper.insert(tx);
    }

    private int safe(Integer value) { return value == null ? 0 : value; }
}
