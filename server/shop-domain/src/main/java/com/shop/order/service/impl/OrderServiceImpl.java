package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.dto.*;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.PaymentLog;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.PaymentLogMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.OrderService;
import com.shop.order.service.OrderPaymentService;
import com.shop.order.service.OrderCancellationService;
import com.shop.order.service.RefundCompletionService;
import com.shop.order.service.WxPayService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import com.shop.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum;
import com.wechat.pay.java.service.refund.model.Refund;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;
    private final UserAddressMapper userAddressMapper;
    private final UserAddressService userAddressService;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final OrderPaymentService orderPaymentService;
    private final WxPayService wxPayService;
    private final RefundCompletionService refundCompletionService;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupBuyGroupMapper groupBuyGroupMapper;
    private final OrderCancellationService orderCancellationService;
    private final PlatformTransactionManager transactionManager;

    private static final java.util.regex.Pattern SHIP_NO_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9]{5,30}$");
    private static final int CREATE_LOCK_TTL_SECONDS = 60;
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    // ==================== preview ====================

    @Override
    public OrderPreviewVO preview(Long userId, OrderPreviewRequest req) {
        return preview(userId, null, req);
    }

    @Override
    public OrderPreviewVO preview(Long userId, Long merchantId, OrderPreviewRequest req) {
        // 校验地址
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, req.getAddressId())
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        // 查购物车项
        List<CartItem> cartItems = cartItemMapper.selectBatchIds(req.getCartItemIds());
        if (cartItems.isEmpty() || cartItems.size() != req.getCartItemIds().size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
        }
        for (CartItem ci : cartItems) {
            if (!ci.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
            }
        }
        validateMerchantScope(merchantId, cartItems);

        // 本项目只支持单个商家结算；预览接口也必须与创建订单保持一致。
        validateSingleMerchantCheckout(cartItems);

        // 批量查 SKU / Product / Merchant
        List<Long> skuIds = cartItems.stream().map(CartItem::getSkuId).distinct().collect(Collectors.toList());
        Map<Long, ProductSku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));

        List<Long> productIds = cartItems.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> merchantIds = cartItems.stream().map(CartItem::getMerchantId).distinct().collect(Collectors.toList());
        Map<Long, Merchant> merchantMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantMapper.selectList(new LambdaQueryWrapper<Merchant>().in(Merchant::getId, merchantIds))
                    .forEach(m -> merchantMap.put(m.getId(), m));
        }

        // 按 merchant 分组
        Map<Long, List<CartItem>> grouped = cartItems.stream()
                .collect(Collectors.groupingBy(CartItem::getMerchantId));

        List<OrderPreviewVO.MerchantGroup> groups = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<Long, List<CartItem>> entry : grouped.entrySet()) {
            Long mid = entry.getKey();
            Merchant merchant = merchantMap.get(mid);
            OrderPreviewVO.MerchantGroup g = new OrderPreviewVO.MerchantGroup();
            g.setMerchantId(mid);
            g.setMerchantName(merchant != null ? merchant.getName() : "");
            g.setItems(new ArrayList<>());
            BigDecimal groupTotal = BigDecimal.ZERO;

            for (CartItem ci : entry.getValue()) {
                OrderPreviewVO.PreviewItem pi = new OrderPreviewVO.PreviewItem();
                pi.setCartItemId(ci.getId());
                pi.setSkuId(ci.getSkuId());
                pi.setProductId(ci.getProductId());
                pi.setQuantity(ci.getQuantity());

                ProductSku sku = skuMap.get(ci.getSkuId());
                Product product = productMap.get(ci.getProductId());

                // 可用性算法
                if (sku == null || !Integer.valueOf(1).equals(sku.getActive())) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("规格已下架");
                } else if (product == null) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("商品已删除");
                } else if (!Objects.equals(sku.getProductId(), product.getId())
                        || !Objects.equals(ci.getMerchantId(), product.getMerchantId())) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("商品归属已变化");
                } else if (product.getStatus() == null || product.getStatus() != 1) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("商品已下架");
                } else if (sku.getStock() != null && sku.getStock() < ci.getQuantity()) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("库存不足,仅剩 " + sku.getStock() + " 件");
                } else {
                    pi.setAvailable(true);
                    pi.setProductName(product.getName());
                    pi.setMainImage(product.getMainImage());
                    pi.setSpecText(sku.getSpecText());
                    pi.setUnitPrice(sku.getPrice());
                    pi.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
                    groupTotal = groupTotal.add(pi.getSubtotal());
                }
                g.getItems().add(pi);
            }

            g.setTotalAmount(groupTotal);
            g.setFreightAmount(BigDecimal.ZERO);
            g.setDiscountAmount(BigDecimal.ZERO);
            g.setPayAmount(groupTotal);
            groups.add(g);
            grandTotal = grandTotal.add(groupTotal);
        }

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setGroups(groups);
        vo.setTotalAmount(grandTotal);
        vo.setAddress(new AddressSnapshot(
                address.getReceiver(), address.getPhone(),
                address.getRegion(), address.getDetail()));
        return vo;
    }

    // ==================== create ====================

    @Override
    public List<OrderCreateVO> create(Long userId, OrderCreateRequest req) {
        return create(userId, null, req);
    }

    @Override
    public List<OrderCreateVO> create(Long userId, Long merchantId, OrderCreateRequest req) {
        // Redis 防连点
        String lockKey = "order:create:" + userId;
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, CREATE_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ErrorCode.BIZ_ERROR);
        }

        try {
            List<OrderCreateVO> results = new TransactionTemplate(transactionManager).execute(status -> {
            // 校验地址
            UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getId, req.getAddressId())
                    .eq(UserAddress::getUserId, userId));
            if (address == null) {
                throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
            }

            // 查购物车项 + 归属校验
            List<Long> cartItemIds = req.getCartItemIds().stream().distinct().collect(Collectors.toList());
            List<CartItem> cartItems = cartItemMapper.selectOwnedForUpdate(userId, cartItemIds);
            if (cartItems.isEmpty() || cartItems.size() != cartItemIds.size()) {
                throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
            }
            for (CartItem ci : cartItems) {
                if (!ci.getUserId().equals(userId)) {
                    throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
                }
            }
            validateMerchantScope(merchantId, cartItems);

            // 本项目只支持单个商家结算，不能依赖前端拦截跨商家请求。
            validateSingleMerchantCheckout(cartItems);

            // 批量查 SKU / Product
            List<Long> skuIds = cartItems.stream().map(CartItem::getSkuId).distinct().collect(Collectors.toList());
            Map<Long, ProductSku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                    .collect(Collectors.toMap(ProductSku::getId, s -> s));

            List<Long> productIds = cartItems.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            // 可用性校验（有任何不可用 → 抛 CART_ITEM_INVALID）
            for (CartItem ci : cartItems) {
                ProductSku sku = skuMap.get(ci.getSkuId());
                Product product = productMap.get(ci.getProductId());
                if (sku == null || !Integer.valueOf(1).equals(sku.getActive()) || product == null
                        || !Objects.equals(sku.getProductId(), product.getId())
                        || !Objects.equals(ci.getMerchantId(), product.getMerchantId())
                        || product.getStatus() == null || product.getStatus() != 1
                        || (sku.getStock() != null && sku.getStock() < ci.getQuantity())) {
                    throw new BusinessException(ErrorCode.CART_ITEM_INVALID);
                }
            }

            // 地址快照（适配真实 UserAddress 字段）
            AddressSnapshot addrSnapshot = new AddressSnapshot(
                    address.getReceiver(), address.getPhone(),
                    address.getRegion(), address.getDetail());
            String addrJson = toJson(addrSnapshot);

            // 按 merchant 分组
            Map<Long, List<CartItem>> grouped = cartItems.stream()
                    .collect(Collectors.groupingBy(CartItem::getMerchantId));

            List<OrderCreateVO> createdOrders = new ArrayList<>();

            for (Map.Entry<Long, List<CartItem>> entry : grouped.entrySet()) {
                Long mid = entry.getKey();
                List<CartItem> groupItems = entry.getValue();

                // 生成订单号
                String orderNo = generateOrderNo(userId);

                // 先插入 order
                Order order = new Order();
                order.setOrderNo(orderNo);
                order.setUserId(userId);
                order.setMerchantId(mid);
                order.setStatus(OrderStatus.WAIT_PAY.getCode());
                order.setTotalAmount(BigDecimal.ZERO);
                order.setFreightAmount(BigDecimal.ZERO);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setPayAmount(BigDecimal.ZERO);
                order.setAddressSnapshot(addrJson);
                order.setRemark(req.getRemark() != null ? req.getRemark() : "");
                orderMapper.insert(order);

                BigDecimal totalAmount = BigDecimal.ZERO;

                // 扣库存 + 建 order_item
                for (CartItem ci : groupItems) {
                    ProductSku sku = skuMap.get(ci.getSkuId());
                    Product product = productMap.get(ci.getProductId());

                    // 乐观锁扣库存
                    int affected = skuMapper.deductStock(ci.getSkuId(), ci.getQuantity());
                    if (affected == 0) {
                        throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
                    }

                    BigDecimal unitPrice = sku.getPrice();
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
                    totalAmount = totalAmount.add(subtotal);

                    OrderItem oi = new OrderItem();
                    oi.setOrderId(order.getId());
                    oi.setOrderNo(orderNo);
                    oi.setProductId(product.getId());
                    oi.setSkuId(sku.getId());
                    oi.setProductName(product.getName());
                    oi.setMainImage(product.getMainImage());
                    oi.setSpecText(sku.getSpecText());
                    oi.setUnitPrice(unitPrice);
                    oi.setQuantity(ci.getQuantity());
                    oi.setSubtotal(subtotal);
                    orderItemMapper.insert(oi);
                }

                // 更新订单金额
                order.setTotalAmount(totalAmount);
                order.setPayAmount(totalAmount);
                orderMapper.updateById(order);

                // recalc 每个 product
                Set<Long> distinctProductIds = groupItems.stream()
                        .map(CartItem::getProductId).collect(Collectors.toSet());
                for (Long pid : distinctProductIds) {
                    productService.recalcProduct(pid);
                }

                // 删购物车项
                List<Long> idsToDelete = groupItems.stream().map(CartItem::getId).collect(Collectors.toList());
                cartItemMapper.deleteBatchIds(idsToDelete);

                // build VO
                OrderCreateVO vo = new OrderCreateVO();
                vo.setOrderNo(orderNo);
                vo.setPayAmount(totalAmount);
                createdOrders.add(vo);
            }

            return createdOrders;
            });
            if (results == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            // 订单、库存和购物车变更提交后再调用微信，避免第三方预下单成功而本地事务回滚。
            // 预下单暂时失败时仍返回已落库订单，用户可从订单列表重新支付。
            for (OrderCreateVO vo : results) {
                Order committedOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, vo.getOrderNo()));
                if (committedOrder == null) {
                    log.error("订单事务已提交但无法读取订单, orderNo={}", vo.getOrderNo());
                    continue;
                }
                try {
                    vo.setPayParams(wxPayService.createJsapiPayParams(committedOrder));
                } catch (RuntimeException e) {
                    log.warn("订单已创建但微信预下单失败，可稍后重新支付, orderNo={}", vo.getOrderNo(), e);
                }
            }
            return results;
        } finally {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
        }
    }

    /** 生成 22 位订单号：yyMMddHHmmss(14) + userId后4补零(4) + random(4) */
    private String generateOrderNo(Long userId) {
        Random rnd = new Random();
        String prefix = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String uidPart = String.format("%04d", userId % 10000);
        for (int i = 0; i < 100; i++) {
            String suffix = String.format("%04d", rnd.nextInt(10000));
            String no = prefix + uidPart + suffix;
            if (orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, no)) == 0) {
                return no;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    private void validateSingleMerchantCheckout(List<CartItem> cartItems) {
        Set<Long> merchantIds = cartItems.stream()
                .map(CartItem::getMerchantId)
                .collect(Collectors.toSet());
        if (merchantIds.size() != 1 || merchantIds.contains(null)) {
            throw new BusinessException(ErrorCode.CART_ITEM_CROSS_MERCHANT);
        }
    }

    private void validateMerchantScope(Long merchantId, List<CartItem> cartItems) {
        if (merchantId == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
        }
        if (cartItems.stream().anyMatch(item -> !merchantId.equals(item.getMerchantId()))) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
        }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    // ==================== cancel ====================

    @Override
    public void cancelByUser(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.canCancel(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        closeWechatPaymentBeforeCancellation(order);
        orderCancellationService.cancelByUser(userId, orderNo);
    }

    @Override
    public int cancelExpired(int batchLimit) {
        List<Order> expired = orderMapper.selectExpiredOrders(batchLimit);
        int count = 0;
        for (Order order : expired) {
            try {
                closeWechatPaymentBeforeCancellation(order);
                if (orderCancellationService.cancelExpired(order.getId())) {
                    count++;
                }
            } catch (Exception e) {
                log.error("取消过期订单失败 orderNo={}", order.getOrderNo(), e);
            }
        }
        return count;
    }

    private void closeWechatPaymentBeforeCancellation(Order order) {
        Transaction transaction = wxPayService.queryOrder(order);
        if (transaction != null && transaction.getTradeState() == TradeStateEnum.SUCCESS) {
            if (transaction.getAmount() == null || transaction.getAmount().getTotal() == null
                    || transaction.getAmount().getTotal() != yuanToFen(order.getPayAmount())
                    || transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
                throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_AMOUNT_MISMATCH);
            }
            orderPaymentService.handlePaidCallback(order.getOrderNo(), transaction.getTransactionId(), toJson(transaction));
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED.getCode(), "订单已支付，不能取消");
        }
        if (transaction == null || transaction.getTradeState() == TradeStateEnum.ACCEPT) {
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "无法确认微信支付状态，请稍后重试");
        }
        if (transaction.getTradeState() != TradeStateEnum.CLOSED) {
            wxPayService.closeOrder(order);
        }
    }

    private int yuanToFen(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.PAY_FAILED);
        }
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.UNNECESSARY).intValueExact();
    }

    // ==================== page / detail ====================

    @Override
    public PageResult<OrderListVO> page(Long userId, int page, int size, Integer status) {
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt)
                .orderByDesc(Order::getId);
        if (status != null) {
            q.eq(Order::getStatus, status);
        }

        IPage<Order> pageReq = new Page<>(page, size);
        IPage<Order> result = orderMapper.selectPage(pageReq, q);

        // 批量查 order_item
        List<Long> orderIds = result.getRecords().stream().map(Order::getId).collect(Collectors.toList());
        final Map<Long, List<OrderItem>> itemsByOrderId = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderItem> allItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getOrderId, orderIds)
                            .orderByAsc(OrderItem::getId));
            itemsByOrderId.putAll(allItems.stream()
                    .collect(Collectors.groupingBy(OrderItem::getOrderId)));
        }

        // 批量查 merchant 名（适配：Merchant.name 而非 User.shopName）
        List<Long> merchantIds = result.getRecords().stream().map(Order::getMerchantId).distinct().collect(Collectors.toList());
        final Map<Long, String> merchantNames = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            List<Merchant> merchants = merchantMapper.selectList(
                    new LambdaQueryWrapper<Merchant>().in(Merchant::getId, merchantIds));
            for (Merchant m : merchants) {
                merchantNames.put(m.getId(), m.getName());
            }
        }

        List<Long> groupIds = result.getRecords().stream()
                .filter(o -> Integer.valueOf(1).equals(o.getOrderType()) && o.getGroupBuyGroupId() != null)
                .map(Order::getGroupBuyGroupId)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, GroupBuyGroup> groupMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            groupBuyGroupMapper.selectBatchIds(groupIds).forEach(g -> groupMap.put(g.getId(), g));
        }

        List<OrderListVO> list = result.getRecords().stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setStatusText(OrderStatus.statusText(o.getStatus()));
            vo.setPayAmount(o.getPayAmount());
            vo.setOrderType(o.getOrderType());
            vo.setGroupBuyGroupId(o.getGroupBuyGroupId());
            GroupBuyGroup group = groupMap.get(o.getGroupBuyGroupId());
            if (group != null) {
                vo.setGroupBuyRequiredCount(group.getRequiredCount());
                vo.setGroupBuyPaidCount(group.getPaidCount());
                if (group.getExpireAt() != null) {
                    vo.setGroupBuyExpireAt(group.getExpireAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                }
            }
            vo.setMerchantId(o.getMerchantId());
            vo.setMerchantName(merchantNames.getOrDefault(o.getMerchantId(), ""));
            vo.setCreatedAt(o.getCreatedAt());

            List<OrderItem> items = itemsByOrderId.getOrDefault(o.getId(), List.of());
            int totalQty = items.stream().mapToInt(OrderItem::getQuantity).sum();
            vo.setItemCount(totalQty);
            vo.setItemSummary(items.stream()
                    .limit(3).map(OrderItem::getProductName)
                    .collect(Collectors.joining("、"))
                    + (items.size() > 3 ? " 等" : ""));
            if (!items.isEmpty()) {
                vo.setFirstItemImage(items.get(0).getMainImage());
            }
            vo.setItems(items.stream().map(i -> {
                OrderListVO.OrderItemVO iv = new OrderListVO.OrderItemVO();
                iv.setProductId(i.getProductId());
                iv.setSkuId(i.getSkuId());
                iv.setProductName(i.getProductName());
                iv.setMainImage(i.getMainImage());
                iv.setSpecText(i.getSpecText());
                iv.setUnitPrice(i.getUnitPrice());
                iv.setQuantity(i.getQuantity());
                iv.setSubtotal(i.getSubtotal());
                return iv;
            }).collect(Collectors.toList()));
            if (o.getStatus() == OrderStatus.WAIT_PAY.getCode() && o.getCreatedAt() != null) {
                vo.setExpireAt(o.getCreatedAt().plusMinutes(30)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            RefundApplication refund = latestRefund(o.getOrderNo());
            if (refund != null) {
                vo.setRefundStatus(refund.getStatus());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public OrderDetailVO detail(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return buildOrderDetailVO(order);
    }

    @Override
    public OrderDetailVO merchantDetail(Long merchantId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getMerchantId, merchantId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return buildOrderDetailVO(order);
    }

    @Override
    public OrderDetailVO adminDetail(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return buildOrderDetailVO(order);
    }

    private OrderDetailVO buildOrderDetailVO(Order order) {
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        List<OrderDetailVO.OrderItemVO> itemVOs = items.stream().map(i -> {
            OrderDetailVO.OrderItemVO iv = new OrderDetailVO.OrderItemVO();
            iv.setProductId(i.getProductId());
            iv.setSkuId(i.getSkuId());
            iv.setProductName(i.getProductName());
            iv.setMainImage(i.getMainImage());
            iv.setSpecText(i.getSpecText());
            iv.setUnitPrice(i.getUnitPrice());
            iv.setQuantity(i.getQuantity());
            iv.setSubtotal(i.getSubtotal());
            return iv;
        }).collect(Collectors.toList());

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatus.statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setOrderType(order.getOrderType());
        vo.setGroupBuyGroupId(order.getGroupBuyGroupId());
        if (Integer.valueOf(1).equals(order.getOrderType()) && order.getGroupBuyGroupId() != null) {
            GroupBuyGroup group = groupBuyGroupMapper.selectById(order.getGroupBuyGroupId());
            if (group != null) {
                vo.setGroupBuyRequiredCount(group.getRequiredCount());
                vo.setGroupBuyPaidCount(group.getPaidCount());
                if (group.getExpireAt() != null) {
                    vo.setGroupBuyExpireAt(group.getExpireAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                }
            }
        }
        vo.setMerchantId(order.getMerchantId());
        vo.setMerchantName(merchant != null ? merchant.getName() : "");
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPayTime(order.getPayTime());
        vo.setPayTransactionId(order.getPayTransactionId());
        vo.setShipNo(order.getShipNo());
        vo.setShipCompany(order.getShipCompany());
        vo.setShipperCode(order.getShipperCode());
        vo.setShipTime(order.getShipTime());
        vo.setShipReminderAt(order.getShipReminderAt());
        vo.setMerchantContactPhone(merchant == null ? "" : merchant.getContactPhone());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setRemark(order.getRemark());
        vo.setItems(itemVOs);

        RefundApplication refund = latestRefund(order.getOrderNo());
        if (refund != null) {
            vo.setRefundId(refund.getId());
            vo.setRefundStatus(refund.getStatus());
            vo.setRefundReason(refund.getReason());
            vo.setRefundRejectReason(refund.getRejectReason());
            vo.setRefundAmount(refund.getRefundAmount());
            vo.setRefundFailReason(refund.getRefundFailReason());
            vo.setRefundEvidenceUrls(refund.getEvidenceUrls());
            vo.setRefundReturnRequired(refund.getReturnRequired());
            vo.setRefundReturnShipCompany(refund.getReturnShipCompany());
            vo.setRefundReturnShipNo(refund.getReturnShipNo());
            vo.setRefundReturnShipTime(refund.getReturnShipTime());
            vo.setRefundReturnReceivedTime(refund.getReturnReceivedTime());
            vo.setRefundReturnReceiveNote(refund.getReturnReceiveNote());
        }

        // 解析地址快照
        try {
            vo.setAddress(new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(order.getAddressSnapshot(), AddressSnapshot.class));
        } catch (Exception e) {
            vo.setAddress(null);
        }

        if (order.getStatus() == OrderStatus.WAIT_PAY.getCode() && order.getCreatedAt() != null) {
            vo.setExpireAt(order.getCreatedAt().plusMinutes(30)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return vo;
    }

    // ==================== ship / confirmReceive / refund ====================

    @Override
    @Transactional
    public void ship(Long merchantId, String orderNo, String shipNo) {
        ship(merchantId, orderNo, "", "", shipNo);
    }

    @Override
    @Transactional
    public void ship(Long merchantId, String orderNo, String shipCompany, String shipNo) {
        ship(merchantId, orderNo, shipCompany, "", shipNo);
    }

    @Override
    @Transactional
    public void ship(Long merchantId, String orderNo, String shipCompany, String shipperCode, String shipNo) {
        if (shipNo == null || !SHIP_NO_PATTERN.matcher(shipNo).matches()) {
            throw new BusinessException(ErrorCode.SHIP_NO_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.ship(merchantId, orderNo, shipCompany == null ? "" : shipCompany.trim(),
                shipperCode == null ? "" : shipperCode.trim().toUpperCase(), shipNo, now);
        if (affected == 0) {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            if (order == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            if (!order.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ErrorCode.ORDER_NOT_YOUR_MERCHANT);
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_WAIT_SHIP);
        }
    }

    @Override
    @Transactional
    public void confirmReceive(Long userId, String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.confirmReceive(userId, orderNo, now);
        if (affected == 0) {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            if (order == null || !order.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_WAIT_RECEIVE);
        }
    }

    @Override
    @Transactional
    public void remindShip(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.WAIT_SHIP.getCode()
                && order.getStatus() != OrderStatus.GROUP_SUCCESS.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        if (orderMapper.remindShip(userId, orderNo, LocalDateTime.now()) == 0) {
            throw new BusinessException(ErrorCode.ORDER_REMINDER_TOO_FREQUENT);
        }
    }

    @Override
    @Transactional
    public void refundApply(Long userId, String orderNo, String reason) {
        RefundApplyRequest req = new RefundApplyRequest();
        req.setReason(reason);
        refundApply(userId, orderNo, req);
    }

    @Override
    @Transactional
    public void refundApply(Long userId, String orderNo, RefundApplyRequest req) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId)
                .last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有待处理申请阻止再次申请；被拒绝的申请允许用户重新提交。
        Long count = refundApplicationMapper.selectCount(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, orderNo)
                .in(RefundApplication::getStatus, RefundStatus.PENDING.getCode(), RefundStatus.REFUNDING.getCode(),
                        RefundStatus.WAIT_RETURN_SHIP.getCode(), RefundStatus.WAIT_RETURN_RECEIVE.getCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_EXISTS);
        }

        int st = order.getStatus();
        RefundApplication latest = latestRefund(orderNo);
        boolean retryAfterRefundFailure = st == OrderStatus.CANCELLED.getCode()
                && "REFUNDED".equals(order.getCancelReason())
                && latest != null && latest.getStatus() == RefundStatus.FAILED.getCode();
        if (!retryAfterRefundFailure && st != OrderStatus.WAIT_SHIP.getCode()
                && st != OrderStatus.GROUP_SUCCESS.getCode()
                && st != OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode()
                && st != OrderStatus.WAIT_RECEIVE.getCode()
                && st != OrderStatus.FINISHED.getCode()) {
            throw new BusinessException(ErrorCode.REFUND_ORDER_NOT_REFUNDABLE);
        }

        BigDecimal refundAmount = req == null || req.getRefundAmount() == null
                ? order.getPayAmount() : req.getRefundAmount();
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0
                || refundAmount.compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "退款金额不能超过订单实付金额");
        }
        BigDecimal refundedAmount = refundApplicationMapper.selectList(new LambdaQueryWrapper<RefundApplication>()
                        .eq(RefundApplication::getOrderNo, orderNo)
                        .eq(RefundApplication::getStatus, RefundStatus.SUCCESS.getCode()))
                .stream()
                .map(RefundApplication::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundAmount.add(refundedAmount).compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "退款金额超过剩余可退金额");
        }

        RefundApplication app = new RefundApplication();
        app.setOrderNo(orderNo);
        app.setOutRefundNo("RF_" + orderNo + "_"
                + UUID.randomUUID().toString().replace("-", ""));
        app.setUserId(userId);
        app.setMerchantId(order.getMerchantId());
        app.setReason(req == null || req.getReason() == null ? "" : req.getReason().trim());
        app.setEvidenceUrls(req == null || req.getEvidenceUrls() == null ? List.of()
                : req.getEvidenceUrls().stream().filter(Objects::nonNull).map(String::trim)
                .filter(value -> !value.isEmpty()).distinct().toList());
        app.setRefundAmount(refundAmount);
        app.setStatus(RefundStatus.PENDING.getCode());
        app.setAutoRefund(0);
        app.setReturnRequired((st == OrderStatus.WAIT_RECEIVE.getCode()
                || st == OrderStatus.FINISHED.getCode()) ? 1 : 0);
        app.setReturnShipCompany("");
        app.setReturnShipNo("");
        app.setReturnReceiveNote("");
        refundApplicationMapper.insert(app);
    }

    @Override
    @Transactional
    public void submitReturnShipment(Long userId, Long refundId, ReturnShipmentRequest req) {
        RefundApplication app = refundApplicationMapper.selectOne(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getId, refundId)
                .eq(RefundApplication::getUserId, userId)
                .last("FOR UPDATE"));
        if (app == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(app.getReturnRequired())
                || app.getStatus() != RefundStatus.WAIT_RETURN_SHIP.getCode()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_PENDING);
        }
        app.setReturnShipCompany(req.getShipCompany().trim());
        app.setReturnShipNo(req.getShipNo().trim());
        app.setReturnShipTime(LocalDateTime.now());
        app.setStatus(RefundStatus.WAIT_RETURN_RECEIVE.getCode());
        refundApplicationMapper.updateById(app);
    }

    @Override
    @Transactional
    public void refundApprove(Long merchantId, Long refundId, boolean approved, String rejectReason) {
        RefundApplication app = refundApplicationMapper.selectOne(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getId, refundId)
                .last("FOR UPDATE"));
        if (app == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        if (!app.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.REFUND_NOT_YOUR_MERCHANT);
        }
        boolean retryingFailedRefund = app.getStatus() == RefundStatus.FAILED.getCode();
        if (app.getStatus() != RefundStatus.PENDING.getCode() && !retryingFailedRefund) {
            throw new BusinessException(ErrorCode.REFUND_NOT_PENDING);
        }
        if (retryingFailedRefund && !approved) {
            throw new BusinessException(ErrorCode.REFUND_NOT_PENDING);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!approved) {
            app.setStatus(RefundStatus.REJECTED.getCode());
            app.setRejectReason(rejectReason != null ? rejectReason : "");
            app.setUpdatedAt(now);
            refundApplicationMapper.updateById(app);
            return;
        }

        // 已发货订单须先完成退货物流与商家验货，再发起原路退款。
        if (Integer.valueOf(1).equals(app.getReturnRequired()) && app.getReturnReceivedTime() == null) {
            app.setStatus(RefundStatus.WAIT_RETURN_SHIP.getCode());
            app.setUpdatedAt(now);
            refundApplicationMapper.updateById(app);
            return;
        }

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, app.getOrderNo())
                .last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "订单没有可退款的支付金额");
        }

        // 退款单号在申请创建时生成，重试时保持不变，微信侧以此保证幂等。
        String outRefundNo = app.getOutRefundNo();
        if (outRefundNo == null || outRefundNo.isBlank()) {
            outRefundNo = "RF_" + app.getId() + "_" + order.getOrderNo();
            app.setOutRefundNo(outRefundNo);
        }
        if (app.getRefundAmount() == null) {
            app.setRefundAmount(order.getPayAmount());
        }
        if (app.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0
                || app.getRefundAmount().compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "退款金额不合法");
        }
        app.setStatus(RefundStatus.REFUNDING.getCode());
        app.setRefundFailReason("");
        app.setUpdatedAt(now);
        refundApplicationMapper.updateById(app);

        // 微信退款请求使用固定 out_refund_no，可安全重试；只有微信受理后才推进订单状态。
        Refund refund = wxPayService.createRefund(order, outRefundNo, app.getReason(), app.getRefundAmount());
        if (refund == null || refund.getStatus() == null) {
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "微信未返回退款状态");
        }
        app.setWxRefundId(refund.getRefundId() == null ? "" : refund.getRefundId());
        boolean fullRefund = app.getRefundAmount().compareTo(order.getPayAmount()) >= 0;
        if (refund.getStatus() == com.wechat.pay.java.service.refund.model.Status.SUCCESS) {
            app.setStatus(RefundStatus.SUCCESS.getCode());
            app.setRefundTime(now);
            if (fullRefund) refundCompletionService.completeIfFullRefund(app, order, now);
        } else if (refund.getStatus() == com.wechat.pay.java.service.refund.model.Status.PROCESSING) {
            app.setStatus(RefundStatus.REFUNDING.getCode());
            // 微信仅受理时资金尚未到账；保留订单状态，发货 SQL 会根据退款单状态原子拦截。
        } else {
            app.setStatus(RefundStatus.FAILED.getCode());
            app.setRefundFailReason("微信退款状态：" + refund.getStatus().name());
        }
        app.setUpdatedAt(now);
        refundApplicationMapper.updateById(app);
    }

    @Override
    @Transactional
    public void confirmReturnReceived(Long merchantId, Long refundId, String note) {
        RefundApplication app = refundApplicationMapper.selectOne(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getId, refundId)
                .eq(RefundApplication::getMerchantId, merchantId)
                .last("FOR UPDATE"));
        if (app == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(app.getReturnRequired())
                || app.getStatus() != RefundStatus.WAIT_RETURN_RECEIVE.getCode()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_PENDING);
        }
        app.setReturnReceivedTime(LocalDateTime.now());
        app.setReturnReceiveNote(note == null ? "" : note.trim());
        // 复用退款请求的幂等键与金额校验逻辑；已验货标记会让 approve 流程直接发起微信退款。
        app.setStatus(RefundStatus.PENDING.getCode());
        refundApplicationMapper.updateById(app);
        refundApprove(merchantId, refundId, true, null);
    }

    private RefundApplication latestRefund(String orderNo) {
        return refundApplicationMapper.selectOne(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, orderNo)
                .orderByDesc(RefundApplication::getId)
                .last("LIMIT 1"));
    }

    // ==================== repay ====================

    @Override
    public OrderCreateVO repay(Long userId, String orderNo) {
        return repay(userId, null, orderNo);
    }

    @Override
    public OrderCreateVO repay(Long userId, Long merchantId, String orderNo) {
        if (merchantId != null) {
            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant == null) {
                throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
            }
            if (!Integer.valueOf(1).equals(merchant.getStatus())) {
                throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
            }
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (merchantId != null && !merchantId.equals(order.getMerchantId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_REPAYABLE);
        }
        // 超时 30 分钟不可再支付
        if (order.getCreatedAt() != null
                && order.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_REPAYABLE);
        }

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderNo(orderNo);
        vo.setPayAmount(order.getPayAmount());

        vo.setPayParams(wxPayService.createJsapiPayParams(order));
        return vo;
    }
}
