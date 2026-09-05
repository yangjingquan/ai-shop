package com.shop.bundle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.bundle.dto.*;
import com.shop.bundle.entity.BundleActivity;
import com.shop.bundle.entity.BundleItem;
import com.shop.bundle.mapper.BundleActivityMapper;
import com.shop.bundle.mapper.BundleItemMapper;
import com.shop.bundle.service.BundleService;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.dto.*;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.WxPayService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import com.shop.seckill.mapper.SeckillSkuMapper;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BundleServiceImpl implements BundleService {
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final BundleActivityMapper activityMapper;
    private final BundleItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final MerchantMapper merchantMapper;
    private final CartItemMapper cartItemMapper;
    private final UserAddressMapper addressMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductService productService;
    private final MarketingFeatureService featureService;
    private final SeckillSkuMapper seckillSkuMapper;
    private final WxPayService wxPayService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public List<BundleActivityVO> merchantList(Long merchantId) {
        return activityMapper.selectList(new LambdaQueryWrapper<BundleActivity>()
                        .eq(BundleActivity::getMerchantId, merchantId)
                        .orderByDesc(BundleActivity::getId))
                .stream().map(item -> toVO(item, false)).toList();
    }

    @Override
    public BundleActivityVO merchantGet(Long merchantId, Long id) {
        return toVO(ownedActivity(merchantId, id), false);
    }

    @Override
    @Transactional
    public Long save(Long merchantId, Long operatorId, Long id, BundleActivityRequest request) {
        if (request.getStartAt() == null || request.getEndAt() == null
                || !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "套餐活动时间不合法");
        }
        List<Long> itemProductIds = request.getItemProductIds() == null ? List.of()
                : request.getItemProductIds().stream().filter(Objects::nonNull).distinct().toList();
        if (itemProductIds.isEmpty() || itemProductIds.contains(request.getMainProductId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "请配置至少一个不同于主商品的搭配商品");
        }
        List<Long> productIds = new ArrayList<>();
        productIds.add(request.getMainProductId());
        productIds.addAll(itemProductIds);
        Map<Long, Product> products = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        if (products.size() != productIds.size() || products.values().stream()
                .anyMatch(p -> !merchantId.equals(p.getMerchantId()) || !Integer.valueOf(1).equals(p.getStatus()))) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        rejectConflictingProducts(merchantId, products.values());

        BundleActivity activity = id == null ? new BundleActivity() : ownedActivity(merchantId, id);
        if (id == null) activity.setMerchantId(merchantId);
        if (request.getStatus() == 1) {
            Long activeSameProduct = activityMapper.selectCount(new LambdaQueryWrapper<BundleActivity>()
                    .eq(BundleActivity::getMerchantId, merchantId)
                    .eq(BundleActivity::getMainProductId, request.getMainProductId())
                    .eq(BundleActivity::getStatus, 1)
                    .lt(BundleActivity::getStartAt, request.getEndAt())
                    .gt(BundleActivity::getEndAt, request.getStartAt())
                    .ne(id != null, BundleActivity::getId, id));
            if (activeSameProduct != null && activeSameProduct > 0) {
                throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "同一主商品只能存在一个启用套餐");
            }
        }
        if (request.getStatus() == null || request.getStatus() < 0 || request.getStatus() > 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        activity.setName(request.getName().trim());
        activity.setMainProductId(request.getMainProductId());
        activity.setDiscountAmount(request.getDiscountAmount());
        activity.setStartAt(request.getStartAt());
        activity.setEndAt(request.getEndAt());
        activity.setStatus(request.getStatus());
        if (id == null) {
            activity.setCreatedBy(operatorId);
            activityMapper.insert(activity);
        } else {
            activityMapper.updateById(activity);
            itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BundleItem>()
                    .eq(BundleItem::getBundleActivityId, id).set(BundleItem::getDeleted, 1));
        }
        int sort = 1;
        for (Long productId : itemProductIds) {
            BundleItem item = new BundleItem();
            item.setBundleActivityId(activity.getId());
            item.setMerchantId(merchantId);
            item.setProductId(productId);
            item.setRequired(0);
            item.setSort(sort++);
            itemMapper.insert(item);
        }
        return activity.getId();
    }

    @Override
    @Transactional
    public void delete(Long merchantId, Long id) {
        BundleActivity activity = ownedActivity(merchantId, id);
        activity.setStatus(2);
        activityMapper.updateById(activity);
    }

    @Override
    public BundleActivityVO findActiveForProduct(Long merchantId, Long productId) {
        if (!featureService.isEnabled(merchantId, MarketingActivityCode.BUNDLE)) return null;
        BundleActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<BundleActivity>()
                .eq(BundleActivity::getMerchantId, merchantId)
                .eq(BundleActivity::getMainProductId, productId)
                .eq(BundleActivity::getStatus, 1)
                .le(BundleActivity::getStartAt, LocalDateTime.now())
                .ge(BundleActivity::getEndAt, LocalDateTime.now())
                .orderByDesc(BundleActivity::getId)
                .last("LIMIT 1"));
        return activity == null ? null : toVO(activity, true);
    }

    @Override
    public BundleActivityVO publicGet(Long merchantId, Long id) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.BUNDLE);
        BundleActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<BundleActivity>()
                .eq(BundleActivity::getId, id).eq(BundleActivity::getMerchantId, merchantId)
                .eq(BundleActivity::getStatus, 1)
                .le(BundleActivity::getStartAt, LocalDateTime.now())
                .ge(BundleActivity::getEndAt, LocalDateTime.now()));
        if (activity == null) throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND);
        return toVO(activity, true);
    }

    @Override
    public BundlePreviewVO preview(Long merchantId, BundlePreviewRequest request) {
        BundleActivity activity = activeActivity(merchantId, request.getBundleId());
        Selection selection = selectSkus(activity, request.getMainSkuId(), request.getItemSkuIds());
        return toPreview(activity, selection.items());
    }

    @Override
    @Transactional
    public BundleCartResult addToCart(Long userId, Long merchantId, BundleCartRequest request) {
        BundleActivity activity = activeActivity(merchantId, request.getBundleId());
        Selection selection = selectSkus(activity, request.getMainSkuId(), request.getItemSkuIds());
        String groupId = UUID.randomUUID().toString().replace("-", "");
        List<Long> ids = new ArrayList<>();
        for (ProductSku sku : selection.items()) {
            CartItem item = new CartItem();
            Product product = productMapper.selectById(sku.getProductId());
            item.setUserId(userId);
            item.setMerchantId(merchantId);
            item.setProductId(product.getId());
            item.setSkuId(sku.getId());
            item.setQuantity(1);
            item.setBundleGroupId(groupId);
            item.setBundleActivityId(activity.getId());
            cartItemMapper.insert(item);
            ids.add(item.getId());
        }
        BundleCartResult result = new BundleCartResult();
        result.setBundleGroupId(groupId);
        result.setBundleActivityId(activity.getId());
        result.setCartItemIds(ids);
        return result;
    }

    @Override
    public boolean containsBundleCartItems(Long userId, Long merchantId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) return false;
        return cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getMerchantId, merchantId)
                .in(CartItem::getId, cartItemIds)
                .isNotNull(CartItem::getBundleGroupId)) > 0;
    }

    @Override
    public OrderPreviewVO previewOrder(Long userId, Long merchantId, OrderPreviewRequest request) {
        BundleCartContext context = loadCartContext(userId, merchantId, request.getCartItemIds());
        if (request.getBundleGroupId() != null && !request.getBundleGroupId().equals(context.groupId)) {
            throw new BusinessException(ErrorCode.CART_ITEM_INVALID);
        }
        UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, request.getAddressId()).eq(UserAddress::getUserId, userId));
        if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        BundlePreviewVO bundle = toPreview(context.activity, context.skus);
        Merchant merchant = merchantMapper.selectById(merchantId);
        OrderPreviewVO.MerchantGroup group = new OrderPreviewVO.MerchantGroup();
        group.setMerchantId(merchantId);
        group.setMerchantName(merchant == null ? "" : merchant.getName());
        List<OrderPreviewVO.PreviewItem> items = bundle.getItems().stream().map(item -> {
            OrderPreviewVO.PreviewItem pi = new OrderPreviewVO.PreviewItem();
            pi.setCartItemId(context.cartItems.stream().filter(ci -> ci.getSkuId().equals(item.getSkuId())).findFirst().map(CartItem::getId).orElse(null));
            pi.setProductId(item.getProductId());
            pi.setSkuId(item.getSkuId());
            pi.setProductName(item.getProductName());
            pi.setMainImage(item.getMainImage());
            pi.setSpecText(item.getSpecText());
            pi.setQuantity(1);
            pi.setUnitPrice(item.getUnitPrice());
            pi.setSubtotal(item.getUnitPrice());
            pi.setAvailable(true);
            pi.setBundleGroupId(context.groupId);
            return pi;
        }).toList();
        group.setItems(items);
        group.setTotalAmount(bundle.getOriginalAmount());
        group.setFreightAmount(BigDecimal.ZERO);
        group.setDiscountAmount(bundle.getBundleDiscountAmount());
        group.setPayAmount(bundle.getPayAmount());
        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setGroups(List.of(group));
        vo.setTotalAmount(bundle.getOriginalAmount());
        vo.setDiscountAmount(bundle.getBundleDiscountAmount());
        vo.setPayAmount(bundle.getPayAmount());
        vo.setBundleActivityId(bundle.getBundleId());
        vo.setBundleName(bundle.getBundleName());
        vo.setBundleDiscountAmount(bundle.getBundleDiscountAmount());
        vo.setBundleSnapshotJson(bundleSnapshot(context.activity, bundle));
        vo.setCouponMessage("搭配购套餐不参与其他优惠");
        vo.setAddress(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail()));
        return vo;
    }

    @Override
    public List<OrderCreateVO> createOrder(Long userId, Long merchantId, OrderCreateRequest request) {
        BundleCartContext context = loadCartContext(userId, merchantId, request.getCartItemIds());
        if (request.getBundleGroupId() != null && !request.getBundleGroupId().equals(context.groupId)) {
            throw new BusinessException(ErrorCode.CART_ITEM_INVALID);
        }
        UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, request.getAddressId()).eq(UserAddress::getUserId, userId));
        if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        List<OrderCreateVO> results = new TransactionTemplate(transactionManager).execute(status -> {
            String orderNo = generateOrderNo(userId);
            BundlePreviewVO bundle = toPreview(context.activity, context.skus);
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setMerchantId(merchantId);
            order.setStatus(OrderStatus.WAIT_PAY.getCode());
            order.setOrderType(4);
            order.setBundleActivityId(bundle.getBundleId());
            order.setTotalAmount(bundle.getOriginalAmount());
            order.setFreightAmount(BigDecimal.ZERO);
            order.setDiscountAmount(bundle.getBundleDiscountAmount());
            order.setBundleDiscountAmount(bundle.getBundleDiscountAmount());
            order.setBundleSnapshotJson(bundleSnapshot(context.activity, bundle));
            order.setPayAmount(bundle.getPayAmount());
            order.setAddressSnapshot(toJson(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail())));
            order.setRemark(request.getRemark() == null ? "" : request.getRemark());
            orderMapper.insert(order);
            for (CartItem cartItem : context.cartItems) {
                Product product = productMapper.selectById(cartItem.getProductId());
                ProductSku sku = skuMapper.selectById(cartItem.getSkuId());
                if (skuMapper.deductStock(sku.getId(), 1) == 0) throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId()); item.setOrderNo(orderNo);
                item.setProductId(product.getId()); item.setSkuId(sku.getId());
                item.setProductName(product.getName()); item.setMainImage(product.getMainImage());
                item.setSpecText(sku.getSpecText()); item.setUnitPrice(sku.getPrice());
                item.setQuantity(1); item.setSubtotal(sku.getPrice()); item.setBundleGroupId(context.groupId);
                orderItemMapper.insert(item);
                productService.recalcProduct(product.getId());
            }
            cartItemMapper.deleteBatchIds(context.cartItems.stream().map(CartItem::getId).toList());
            OrderCreateVO vo = new OrderCreateVO(); vo.setOrderNo(orderNo); vo.setPayAmount(order.getPayAmount());
            return List.of(vo);
        });
        if (results == null) throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        for (OrderCreateVO vo : results) {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, vo.getOrderNo()));
            try { vo.setPayParams(wxPayService.createJsapiPayParams(order)); } catch (RuntimeException ignored) { }
        }
        return results;
    }

    private BundleCartContext loadCartContext(Long userId, Long merchantId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(ErrorCode.CART_ITEM_EMPTY);
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getMerchantId, merchantId)
                .in(CartItem::getId, ids));
        if (cartItems.size() != ids.stream().distinct().count() || cartItems.stream().anyMatch(ci -> ci.getBundleGroupId() == null)) {
            throw new BusinessException(ErrorCode.CART_ITEM_INVALID);
        }
        String groupId = cartItems.get(0).getBundleGroupId();
        if (cartItems.stream().anyMatch(ci -> !groupId.equals(ci.getBundleGroupId()) || !Objects.equals(ci.getBundleActivityId(), cartItems.get(0).getBundleActivityId()))) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "套餐商品必须整体结算");
        }
        BundleActivity activity = activeActivity(merchantId, cartItems.get(0).getBundleActivityId());
        List<ProductSku> skus = skuMapper.selectBatchIds(cartItems.stream().map(CartItem::getSkuId).toList());
        if (skus.size() != cartItems.size() || skus.stream().anyMatch(s -> !Integer.valueOf(1).equals(s.getActive()) || s.getStock() == null || s.getStock() < 1)) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        Set<Long> selectedSkuIds = skus.stream().map(ProductSku::getId).collect(Collectors.toSet());
        if (cartItems.stream().anyMatch(ci -> !selectedSkuIds.contains(ci.getSkuId()))) throw new BusinessException(ErrorCode.CART_ITEM_INVALID);
        Selection selection = selectSkus(activity, skus.stream().filter(s -> s.getProductId().equals(activity.getMainProductId())).findFirst().map(ProductSku::getId).orElse(0L),
                skus.stream().filter(s -> !s.getProductId().equals(activity.getMainProductId())).map(ProductSku::getId).toList());
        if (selection.items().size() != cartItems.size()) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "套餐商品不完整，请重新选择");
        return new BundleCartContext(groupId, activity, cartItems, selection.items());
    }

    private BundleActivity activeActivity(Long merchantId, Long id) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.BUNDLE);
        BundleActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<BundleActivity>()
                .eq(BundleActivity::getId, id).eq(BundleActivity::getMerchantId, merchantId).eq(BundleActivity::getStatus, 1)
                .le(BundleActivity::getStartAt, LocalDateTime.now()).ge(BundleActivity::getEndAt, LocalDateTime.now()));
        if (activity == null) throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND);
        return activity;
    }

    private BundleActivity ownedActivity(Long merchantId, Long id) {
        BundleActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<BundleActivity>()
                .eq(BundleActivity::getId, id).eq(BundleActivity::getMerchantId, merchantId));
        if (activity == null) throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND);
        return activity;
    }

    private Selection selectSkus(BundleActivity activity, Long mainSkuId, List<Long> itemSkuIds) {
        List<Long> skuIds = new ArrayList<>(); skuIds.add(mainSkuId);
        if (itemSkuIds != null) skuIds.addAll(itemSkuIds);
        if (skuIds.stream().anyMatch(Objects::isNull) || skuIds.size() != new HashSet<>(skuIds).size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "套餐规格选择重复");
        }
        List<ProductSku> skus = skuMapper.selectBatchIds(skuIds);
        if (skus.size() != skuIds.size()) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        Product main = productMapper.selectById(activity.getMainProductId());
        if (main == null || !activity.getMerchantId().equals(main.getMerchantId()) || !Integer.valueOf(1).equals(main.getStatus())) throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
        if (skus.stream().anyMatch(s -> !Integer.valueOf(1).equals(s.getActive()) || s.getStock() == null || s.getStock() < 1)) throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        if (skus.get(0).getProductId() == null || !activity.getMainProductId().equals(skus.get(0).getProductId())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "主商品规格不匹配");
        Set<Long> allowedProducts = itemMapper.selectList(new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleActivityId, activity.getId())).stream().map(BundleItem::getProductId).collect(Collectors.toSet());
        if (skus.stream().skip(1).anyMatch(s -> !allowedProducts.contains(s.getProductId()))) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "搭配商品不属于当前套餐");
        return new Selection(skus);
    }

    private BundlePreviewVO toPreview(BundleActivity activity, List<ProductSku> skus) {
        BigDecimal original = skus.stream().map(ProductSku::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BundlePreviewVO vo = new BundlePreviewVO(); vo.setBundleId(activity.getId()); vo.setBundleName(activity.getName());
        vo.setOriginalAmount(original); vo.setBundleDiscountAmount(activity.getDiscountAmount().min(original));
        vo.setPayAmount(original.subtract(vo.getBundleDiscountAmount()).max(BigDecimal.ZERO));
        vo.setItems(skus.stream().map(sku -> {
            Product product = productMapper.selectById(sku.getProductId());
            BundlePreviewVO.Item item = new BundlePreviewVO.Item(); item.setProductId(product.getId()); item.setSkuId(sku.getId());
            item.setProductName(product.getName()); item.setMainImage(product.getMainImage()); item.setSpecText(sku.getSpecText());
            item.setUnitPrice(sku.getPrice()); item.setQuantity(1); item.setStock(sku.getStock()); item.setAvailable(true); return item;
        }).toList());
        return vo;
    }

    private BundleActivityVO toVO(BundleActivity activity, boolean activeOnly) {
        Product main = productMapper.selectById(activity.getMainProductId());
        List<BundleSkuVO> mainSkus = main == null ? List.of() : skuVOs(main.getId());
        List<BundleItemVO> items = itemMapper.selectList(new LambdaQueryWrapper<BundleItem>()
                        .eq(BundleItem::getBundleActivityId, activity.getId()).orderByAsc(BundleItem::getSort))
                .stream().map(item -> { Product p = productMapper.selectById(item.getProductId()); BundleItemVO vo = new BundleItemVO(); vo.setId(item.getId()); vo.setProductId(item.getProductId()); vo.setProductName(p == null ? "" : p.getName()); vo.setMainImage(p == null ? "" : p.getMainImage()); vo.setRequired(item.getRequired()); vo.setSort(item.getSort()); vo.setSkus(p == null ? List.of() : skuVOs(p.getId())); return vo; }).toList();
        BundleActivityVO vo = new BundleActivityVO(); vo.setId(activity.getId()); vo.setName(activity.getName()); vo.setMainProductId(activity.getMainProductId());
        vo.setMainProductName(main == null ? "" : main.getName()); vo.setMainProductImage(main == null ? "" : main.getMainImage()); vo.setMainSkus(mainSkus);
        vo.setDiscountAmount(activity.getDiscountAmount()); vo.setStartAt(activity.getStartAt()); vo.setEndAt(activity.getEndAt()); vo.setStatus(activity.getStatus());
        vo.setStatusText(statusText(activity.getStatus())); vo.setActive(activity.getStatus() == 1 && !LocalDateTime.now().isBefore(activity.getStartAt()) && !LocalDateTime.now().isAfter(activity.getEndAt())); vo.setItems(items); return vo;
    }

    private List<BundleSkuVO> skuVOs(Long productId) { return skuMapper.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId).eq(ProductSku::getActive, 1).orderByAsc(ProductSku::getId)).stream().map(s -> { BundleSkuVO vo = new BundleSkuVO(); vo.setId(s.getId()); vo.setSpecText(s.getSpecText()); vo.setPrice(s.getPrice()); vo.setStock(s.getStock()); vo.setImage(s.getImage()); return vo; }).toList(); }
    private String statusText(Integer status) { return status == null ? "草稿" : status == 1 ? "已启用" : status == 2 ? "已停用" : "草稿"; }
    private void rejectConflictingProducts(Long merchantId, Collection<Product> products) { if (products.stream().anyMatch(p -> Integer.valueOf(1).equals(p.getIsGroupBuy()))) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "团购商品不能加入搭配购"); List<Long> ids = products.stream().map(Product::getId).toList(); if (seckillSkuMapper.countActiveByProducts(merchantId, ids) > 0) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "秒杀商品不能加入搭配购"); }
    private String bundleSnapshot(BundleActivity a, BundlePreviewVO p) { return toJson(Map.of("bundleId", a.getId(), "name", a.getName(), "discountAmount", p.getBundleDiscountAmount(), "items", p.getItems())); }
    private String toJson(Object obj) { try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String generateOrderNo(Long userId) { Random random = new Random(); for (int i = 0; i < 100; i++) { String no = LocalDateTime.now().format(ORDER_TIME) + String.format("%04d", userId % 10000) + String.format("%04d", random.nextInt(10000)); if (orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no)) == 0) return no; } throw new BusinessException(ErrorCode.SYSTEM_ERROR); }
    private record Selection(List<ProductSku> items) {}
    private record BundleCartContext(String groupId, BundleActivity activity, List<CartItem> cartItems, List<ProductSku> skus) {}
}
