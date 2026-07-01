package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final StringRedisTemplate stringRedisTemplate;

    private static final java.util.regex.Pattern SHIP_NO_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9]{5,30}$");

    // ==================== preview ====================

    @Override
    public OrderPreviewVO preview(Long userId, OrderPreviewRequest req) {
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
                if (sku == null) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("规格已下架");
                } else if (product == null) {
                    pi.setAvailable(false);
                    pi.setUnavailableReason("商品已删除");
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
    @Transactional
    public List<OrderCreateVO> create(Long userId, OrderCreateRequest req) {
        // Redis 防连点
        String lockKey = "order:create:" + userId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 5, java.util.concurrent.TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ErrorCode.BIZ_ERROR);
        }

        try {
            // 校验地址
            UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getId, req.getAddressId())
                    .eq(UserAddress::getUserId, userId));
            if (address == null) {
                throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
            }

            // 查购物车项 + 归属校验
            List<CartItem> cartItems = cartItemMapper.selectBatchIds(req.getCartItemIds());
            if (cartItems.isEmpty() || cartItems.size() != req.getCartItemIds().size()) {
                throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
            }
            for (CartItem ci : cartItems) {
                if (!ci.getUserId().equals(userId)) {
                    throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
                }
            }

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
                if (sku == null || product == null
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

            List<OrderCreateVO> results = new ArrayList<>();

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
                OrderCreateVO.PayParams pp = new OrderCreateVO.PayParams();
                pp.setAppId("wx_mock");
                pp.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
                pp.setNonceStr(UUID.randomUUID().toString().substring(0, 16));
                pp.setPackageStr("prepay_id=mock_" + orderNo);
                pp.setSignType("MD5");
                pp.setPaySign("MOCK_SIGN");
                vo.setPayParams(pp);
                results.add(vo);
            }

            return results;
        } finally {
            stringRedisTemplate.delete(lockKey);
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

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    // ==================== cancel ====================

    @Override
    @Transactional
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
        cancelInternal(order, "USER_CANCEL");
    }

    @Override
    public int cancelExpired(int batchLimit) {
        List<Order> expired = orderMapper.selectExpiredOrders(batchLimit);
        int count = 0;
        for (Order order : expired) {
            try {
                cancelInternalInNewTx(order, "TIMEOUT");
                count++;
            } catch (Exception e) {
                log.error("取消过期订单失败 orderNo={}", order.getOrderNo(), e);
            }
        }
        return count;
    }

    /** 在同一事务内取消，供用户取消调用 */
    private void cancelInternal(Order order, String reason) {
        doCancel(order, reason);
    }

    /** 在新事务内取消，供定时任务逐条调用 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelInternalInNewTx(Order order, String reason) {
        doCancel(order, reason);
    }

    private void doCancel(Order order, String reason) {
        // SELECT FOR UPDATE 行锁
        Order locked = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, order.getId())
                .last("FOR UPDATE"));
        if (locked == null || locked.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
            return; // 已被处理
        }

        // 回滚库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            orderMapper.releaseStock(item.getSkuId(), item.getQuantity());
        }

        // recalc 每个 product
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        for (Long pid : productIds) {
            productService.recalcProduct(pid);
        }

        // 更新订单状态
        locked.setStatus(OrderStatus.CANCELLED.getCode());
        locked.setCancelTime(LocalDateTime.now());
        locked.setCancelReason(reason);
        orderMapper.updateById(locked);
    }

    // ==================== page / detail ====================

    @Override
    public PageResult<OrderListVO> page(Long userId, int page, int size, Integer status) {
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
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
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
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

        List<OrderListVO> list = result.getRecords().stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setStatusText(OrderStatus.statusText(o.getStatus()));
            vo.setPayAmount(o.getPayAmount());
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
            if (o.getStatus() == OrderStatus.WAIT_PAY.getCode() && o.getCreatedAt() != null) {
                vo.setExpireAt(o.getCreatedAt().plusMinutes(30)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
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
        vo.setMerchantId(order.getMerchantId());
        vo.setMerchantName(merchant != null ? merchant.getName() : "");
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPayTime(order.getPayTime());
        vo.setPayTransactionId(order.getPayTransactionId());
        vo.setShipNo(order.getShipNo());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setRemark(order.getRemark());
        vo.setItems(itemVOs);

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
        if (shipNo == null || !SHIP_NO_PATTERN.matcher(shipNo).matches()) {
            throw new BusinessException(ErrorCode.SHIP_NO_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.ship(merchantId, orderNo, shipNo, now);
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
    public void refundApply(Long userId, String orderNo, String reason) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 检查是否已有退款申请（任意状态，防止重复申请）
        Long count = refundApplicationMapper.selectCount(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, orderNo));
        if (count > 0) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_EXISTS);
        }

        int st = order.getStatus();
        if (st != OrderStatus.WAIT_SHIP.getCode()
                && st != OrderStatus.WAIT_RECEIVE.getCode()
                && st != OrderStatus.FINISHED.getCode()) {
            throw new BusinessException(ErrorCode.REFUND_ORDER_NOT_REFUNDABLE);
        }

        RefundApplication app = new RefundApplication();
        app.setOrderNo(orderNo);
        app.setUserId(userId);
        app.setMerchantId(order.getMerchantId());
        app.setReason(reason != null ? reason : "");
        app.setStatus(RefundStatus.PENDING.getCode());
        refundApplicationMapper.insert(app);
    }

    @Override
    @Transactional
    public void refundApprove(Long merchantId, Long refundId, boolean approved, String rejectReason) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        if (!app.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.REFUND_NOT_YOUR_MERCHANT);
        }
        if (app.getStatus() != RefundStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_PENDING);
        }

        LocalDateTime now = LocalDateTime.now();
        if (approved) {
            app.setStatus(RefundStatus.APPROVED.getCode());
            app.setUpdatedAt(now);
            refundApplicationMapper.updateById(app);

            // 订单状态改 CANCELLED
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, app.getOrderNo()));
            if (order != null) {
                // 仅 WAIT_SHIP 的退款回滚库存（货未发）
                if (order.getStatus() == OrderStatus.WAIT_SHIP.getCode()) {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
                    for (OrderItem item : items) {
                        orderMapper.releaseStock(item.getSkuId(), item.getQuantity());
                    }
                    Set<Long> pids = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
                    for (Long pid : pids) {
                        productService.recalcProduct(pid);
                    }
                }
                order.setStatus(OrderStatus.CANCELLED.getCode());
                order.setCancelReason("REFUNDED");
                order.setCancelTime(now);
                order.setUpdatedAt(now);
                orderMapper.updateById(order);
            }
        } else {
            app.setStatus(RefundStatus.REJECTED.getCode());
            app.setRejectReason(rejectReason != null ? rejectReason : "");
            app.setUpdatedAt(now);
            refundApplicationMapper.updateById(app);
        }
    }

    // ==================== repay ====================

    @Override
    public OrderCreateVO repay(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
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

        OrderCreateVO.PayParams pp = new OrderCreateVO.PayParams();
        pp.setAppId("wx_mock");
        pp.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        pp.setNonceStr(UUID.randomUUID().toString().substring(0, 16));
        pp.setPackageStr("prepay_id=mock_repay_" + orderNo);
        pp.setSignType("MD5");
        pp.setPaySign("MOCK_SIGN_REPAY");
        vo.setPayParams(pp);
        return vo;
    }
}
