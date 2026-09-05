package com.shop.marketing.controller;

import com.shop.common.response.ApiResult;
import com.shop.marketing.dto.PromotionActivityVO;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.marketing.service.PromotionService;
import com.shop.marketing.dto.PromotionCartProgressRequest;
import com.shop.marketing.dto.PromotionCheckoutResult;
import com.shop.marketing.dto.PromotionPricingItem;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUserHolder;
import jakarta.validation.Valid;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wx/promotions")
@RequiredArgsConstructor
public class WxPromotionController {
    private final PromotionService promotionService;
    private final MarketingFeatureService marketingFeatureService;
    private final WxMerchantResolver wxMerchantResolver;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    @GetMapping("/active")
    public ApiResult<List<PromotionActivityVO>> active(HttpServletRequest request) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(request);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.FULL_REDUCTION);
        return ApiResult.success(promotionService.listActive(merchantId));
    }

    @PostMapping("/cart-progress")
    public ApiResult<PromotionCheckoutResult> cartProgress(@RequestBody @Valid PromotionCartProgressRequest request,
                                                            HttpServletRequest servletRequest) {
        Long merchantId = wxMerchantResolver.requireActiveMerchant(servletRequest);
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.FULL_REDUCTION);
        List<CartItem> cartItems = cartItemMapper.selectBatchIds(request.getCartItemIds());
        if (cartItems.size() != request.getCartItemIds().size() || cartItems.stream().anyMatch(item -> !CurrentUserHolder.get().getUserId().equals(item.getUserId()) || !merchantId.equals(item.getMerchantId()))) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
        }
        Map<Long, Product> products = productMapper.selectBatchIds(cartItems.stream().map(CartItem::getProductId).distinct().toList()).stream().collect(Collectors.toMap(Product::getId, item -> item));
        Map<Long, ProductSku> skus = skuMapper.selectBatchIds(cartItems.stream().map(CartItem::getSkuId).distinct().toList()).stream().collect(Collectors.toMap(ProductSku::getId, item -> item));
        List<PromotionPricingItem> items = cartItems.stream().map(item -> {
            Product product = products.get(item.getProductId()); ProductSku sku = skus.get(item.getSkuId());
            return new PromotionPricingItem(item.getProductId(), product == null ? null : product.getCategoryId(), sku == null ? BigDecimal.ZERO : sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }).toList();
        return ApiResult.success(promotionService.calculate(merchantId, items));
    }
}
