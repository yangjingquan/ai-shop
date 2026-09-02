package com.shop.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.inventory.dto.InventoryAdjustmentRequest;
import com.shop.inventory.dto.InventorySkuVO;
import com.shop.inventory.dto.InventoryTransactionVO;
import com.shop.inventory.entity.InventoryTransaction;
import com.shop.inventory.mapper.InventoryTransactionMapper;
import com.shop.inventory.service.InventoryService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final ProductService productService;

    @Override
    public PageResult<InventorySkuVO> skuPage(Long merchantId, int page, int size,
                                               String keyword, boolean lowStockOnly, int threshold) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safeThreshold = Math.min(Math.max(threshold, 0), 1000000);
        IPage<InventorySkuVO> result = skuMapper.selectMerchantInventoryPage(
                new Page<>(safePage, safeSize), merchantId,
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                lowStockOnly, safeThreshold);
        return PageResult.of(result.getRecords(), result.getTotal(), safePage, safeSize);
    }

    @Override
    public PageResult<InventoryTransactionVO> transactionPage(Long merchantId, int page, int size, Long skuId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        IPage<InventoryTransactionVO> result = transactionMapper.selectMerchantPage(
                new Page<>(safePage, safeSize), merchantId, skuId);
        return PageResult.of(result.getRecords(), result.getTotal(), safePage, safeSize);
    }

    @Override
    @Transactional
    public void adjust(Long merchantId, Long operatorId, InventoryAdjustmentRequest request) {
        if (request == null || request.getSkuId() == null || request.getChangeQty() == null
                || request.getChangeQty() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "库存变更数量不能为 0");
        }
        ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getId, request.getSkuId())
                .eq(ProductSku::getActive, 1)
                .last("FOR UPDATE"));
        if (sku == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || !merchantId.equals(product.getMerchantId())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        int before = sku.getStock() == null ? 0 : sku.getStock();
        long afterLong = (long) before + request.getChangeQty();
        if (afterLong < 0 || afterLong > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "库存变更后必须在 0 到 2147483647 之间");
        }
        int after = (int) afterLong;
        int affected = skuMapper.adjustStock(sku.getId(), before, after);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "库存更新失败，请重试");
        }

        InventoryTransaction tx = new InventoryTransaction();
        tx.setMerchantId(merchantId);
        tx.setProductId(product.getId());
        tx.setSkuId(sku.getId());
        tx.setChangeQty(request.getChangeQty());
        tx.setStockBefore(before);
        tx.setStockAfter(after);
        tx.setOperationType("MANUAL_ADJUST");
        tx.setReferenceNo("");
        tx.setReason(request.getReason() == null ? "" : request.getReason().trim());
        tx.setOperatorId(operatorId);
        transactionMapper.insert(tx);
        productService.recalcProduct(product.getId());
    }
}
