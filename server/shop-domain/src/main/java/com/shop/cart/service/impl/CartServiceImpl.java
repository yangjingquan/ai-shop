package com.shop.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.cart.dto.CartAddRequest;
import com.shop.cart.dto.CartItemVO;
import com.shop.cart.dto.CartUpdateRequest;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.cart.service.CartService;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_CART_ITEM_QUANTITY = 99;
    private static final int MAX_CART_ITEM_COUNT = 100;

    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Transactional
    public Long add(Long userId, CartAddRequest req) {
        return add(userId, null, req);
    }

    @Override
    @Transactional
    public Long add(Long userId, Long merchantId, CartAddRequest req) {
        validateQuantity(req.getQuantity());
        ProductSku sku = skuMapper.selectById(req.getSkuId());
        if (sku == null || !Integer.valueOf(1).equals(sku.getActive())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
        }
        if (merchantId != null && !merchantId.equals(product.getMerchantId())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (sku.getStock() == null || sku.getStock() < req.getQuantity()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        // 行锁 + 数据库唯一约束保证并发加购不会插入重复行。
        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, req.getSkuId())
                .last("FOR UPDATE"));
        if (exist != null) {
            int newQty = exist.getQuantity() + req.getQuantity();
            if (newQty > MAX_CART_ITEM_QUANTITY || newQty > sku.getStock()) {
                throw new BusinessException(newQty > sku.getStock()
                        ? ErrorCode.STOCK_NOT_ENOUGH : ErrorCode.CART_QUANTITY_EXCEEDED);
            }
            exist.setQuantity(newQty);
            cartItemMapper.updateById(exist);
            return exist.getId();
        }

        Long activeCount = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (activeCount != null && activeCount >= MAX_CART_ITEM_COUNT) {
            throw new BusinessException(ErrorCode.CART_FULL);
        }

        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setMerchantId(product.getMerchantId());
        item.setProductId(product.getId());
        item.setSkuId(sku.getId());
        item.setQuantity(req.getQuantity());
        cartItemMapper.insert(item);
        return item.getId();
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> skuIds = items.stream().map(CartItem::getSkuId).distinct().collect(Collectors.toList());
        List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
        List<Long> merchantIds = items.stream().map(CartItem::getMerchantId).distinct().collect(Collectors.toList());

        Map<Long, ProductSku> skuMap = skuIds.isEmpty() ? Map.of()
                : skuMapper.selectList(new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId, skuIds))
                .stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectList(new LambdaQueryWrapper<Product>().in(Product::getId, productIds))
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, String> merchantNames = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            for (Merchant m : merchantMapper.selectList(
                    new LambdaQueryWrapper<Merchant>().in(Merchant::getId, merchantIds))) {
                merchantNames.put(m.getId(), m.getName());
            }
        }

        List<CartItemVO> result = new ArrayList<>(items.size());
        for (CartItem ci : items) {
            CartItemVO vo = new CartItemVO();
            vo.setId(ci.getId());
            vo.setMerchantId(ci.getMerchantId());
            vo.setMerchantName(merchantNames.get(ci.getMerchantId()));
            vo.setProductId(ci.getProductId());
            vo.setSkuId(ci.getSkuId());
            vo.setQuantity(ci.getQuantity());

            ProductSku sku = skuMap.get(ci.getSkuId());
            Product product = productMap.get(ci.getProductId());

            BigDecimal unitPrice = sku != null ? sku.getPrice() : BigDecimal.ZERO;
            vo.setUnitPrice(unitPrice);
            vo.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
            vo.setSpecText(sku != null ? sku.getSpecText() : "");
            vo.setStock(sku != null ? sku.getStock() : 0);

            if (product != null) {
                vo.setProductName(product.getName());
                vo.setMainImage(product.getMainImage());
                vo.setProductStatus(product.getStatus());
            }

            // 可用性判断
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                vo.setAvailable(false);
                vo.setUnavailableReason("OFF_SHELF");
            } else if (sku == null || !Integer.valueOf(1).equals(sku.getActive())) {
                vo.setAvailable(false);
                vo.setUnavailableReason("SKU_GONE");
            } else if (!Objects.equals(sku.getProductId(), product.getId())
                    || !Objects.equals(ci.getMerchantId(), product.getMerchantId())) {
                vo.setAvailable(false);
                vo.setUnavailableReason("MERCHANT_MISMATCH");
            } else if (sku.getStock() == null || sku.getStock() < ci.getQuantity()) {
                vo.setAvailable(false);
                vo.setUnavailableReason("STOCK_NOT_ENOUGH");
            } else {
                vo.setAvailable(true);
                vo.setUnavailableReason("");
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void update(Long userId, Long cartItemId, CartUpdateRequest req) {
        update(userId, null, cartItemId, req);
    }

    @Override
    @Transactional
    public void update(Long userId, Long merchantId, Long cartItemId, CartUpdateRequest req) {
        if (req.getQuantity() > MAX_CART_ITEM_QUANTITY) {
            throw new BusinessException(ErrorCode.CART_QUANTITY_EXCEEDED);
        }
        CartItem item = mustOwn(userId, merchantId, cartItemId);
        if (req.getQuantity() == 0) {
            cartItemMapper.deleteById(item.getId());
            return;
        }
        ProductSku sku = skuMapper.selectById(item.getSkuId());
        if (sku == null || !Integer.valueOf(1).equals(sku.getActive())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (sku.getStock() == null || sku.getStock() < req.getQuantity()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        item.setQuantity(req.getQuantity());
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long cartItemId) {
        delete(userId, null, cartItemId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long merchantId, Long cartItemId) {
        CartItem item = mustOwn(userId, merchantId, cartItemId);
        cartItemMapper.deleteById(item.getId());
    }

    @Override
    @Transactional
    public void deleteBatch(Long userId, List<Long> cartItemIds) {
        deleteBatch(userId, null, cartItemIds);
    }

    @Override
    @Transactional
    public void deleteBatch(Long userId, Long merchantId, List<Long> cartItemIds) {
        List<Long> distinctIds = cartItemIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return;
        }

        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getId, distinctIds));
        if (items.size() != distinctIds.size()
                || (merchantId != null && items.stream().anyMatch(item -> !merchantId.equals(item.getMerchantId())))) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
        }

        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getId, distinctIds));
    }

    private CartItem mustOwn(Long userId, Long merchantId, Long cartItemId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !userId.equals(item.getUserId())
                || (merchantId != null && !merchantId.equals(item.getMerchantId()))) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
        }
        return item;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        if (quantity > MAX_CART_ITEM_QUANTITY) {
            throw new BusinessException(ErrorCode.CART_QUANTITY_EXCEEDED);
        }
    }
}
