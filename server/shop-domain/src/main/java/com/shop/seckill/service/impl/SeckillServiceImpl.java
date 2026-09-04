package com.shop.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.dto.AddressSnapshot;
import com.shop.order.dto.OrderCreateVO;
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
import com.shop.product.dto.ProductDetailVO;
import com.shop.seckill.dto.*;
import com.shop.seckill.entity.SeckillActivity;
import com.shop.seckill.entity.SeckillOrder;
import com.shop.seckill.entity.SeckillSession;
import com.shop.seckill.entity.SeckillSku;
import com.shop.seckill.mapper.SeckillActivityMapper;
import com.shop.seckill.mapper.SeckillOrderMapper;
import com.shop.seckill.mapper.SeckillSessionMapper;
import com.shop.seckill.mapper.SeckillSkuMapper;
import com.shop.seckill.service.SeckillService;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String CREATE_LOCK_PREFIX = "seckill:create:";
    private static final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private final SeckillActivityMapper activityMapper;
    private final SeckillSessionMapper sessionMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserAddressMapper userAddressMapper;
    private final MerchantMapper merchantMapper;
    private final MarketingFeatureService marketingFeatureService;
    private final ProductService productService;
    private final WxPayService wxPayService;
    private final StringRedisTemplate redisTemplate;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    @Override
    public List<SeckillSessionVO> sessions(Long merchantId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.SECKILL);
        String cacheKey = SESSION_CACHE_PREFIX + merchantId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return objectMapper.readValue(cached, new TypeReference<List<SeckillSessionVO>>() {});
        } catch (Exception ex) {
            log.debug("读取秒杀场次缓存失败，降级查询数据库 merchantId={}", merchantId, ex);
        }
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getMerchantId, merchantId)
                .eq(SeckillActivity::getStatus, 1)
                .orderByDesc(SeckillActivity::getId));
        if (activities.isEmpty()) return List.of();
        Map<Long, SeckillActivity> activityMap = activities.stream()
                .collect(Collectors.toMap(SeckillActivity::getId, a -> a));
        List<SeckillSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<SeckillSession>()
                .in(SeckillSession::getActivityId, activityMap.keySet())
                .ge(SeckillSession::getEndAt, now)
                .orderByAsc(SeckillSession::getStartAt)
                .orderByAsc(SeckillSession::getSort));
        List<SeckillSessionVO> result = sessions.stream().map(s -> buildSession(s, activityMap.get(s.getActivityId()), false)).toList();
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.debug("写入秒杀场次缓存失败 merchantId={}", merchantId, ex);
        }
        return result;
    }

    @Override
    public SeckillSessionVO sessionDetail(Long merchantId, Long sessionId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.SECKILL);
        SeckillSession session = loadSession(merchantId, sessionId, false);
        SeckillActivity activity = activityMapper.selectById(session.getActivityId());
        return buildSession(session, activity, true);
    }

    @Override
    public SeckillProductDetailVO productDetail(Long merchantId, Long productId, Long sessionId, Long seckillSkuId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.SECKILL);
        SeckillContext context = loadContext(merchantId, sessionId, seckillSkuId, productId, true);
        SeckillProductDetailVO vo = new SeckillProductDetailVO();
        vo.setSessionId(context.session.getId());
        vo.setSessionName(context.session.getName());
        vo.setActivityId(context.activity.getId());
        vo.setActivityName(context.activity.getName());
        vo.setStartAt(context.session.getStartAt());
        vo.setEndAt(context.session.getEndAt());
        vo.setSessionStatus(sessionStatus(context.session));
        vo.setSessionStatusText(sessionStatusText(vo.getSessionStatus()));
        vo.setSeckillSkuId(context.seckillSku.getId());
        vo.setProductId(context.product.getId());
        vo.setProductName(context.product.getName());
        vo.setSubtitle(context.product.getSubtitle());
        vo.setMainImage(context.product.getMainImage());
        vo.setImages(context.product.getImages() == null ? List.of() : context.product.getImages());
        vo.setDescription(context.product.getDescription());
        vo.setCategoryId(context.product.getCategoryId());
        vo.setTotalSales(context.product.getTotalSales());
        ProductDetailVO baseProduct = productService.publicGet(context.product.getId(), merchantId);
        vo.setSpecs(baseProduct.getSpecs());
        vo.setSkuId(context.productSku.getId());
        vo.setSpecText(context.productSku.getSpecText());
        vo.setSkuPrice(context.productSku.getPrice());
        fillSelected(vo, context.seckillSku, context.productSku);
        List<SeckillSku> configs = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getSessionId, sessionId)
                .eq(SeckillSku::getProductId, context.product.getId())
                .orderByAsc(SeckillSku::getId));
        Map<Long, ProductSku> productSkus = productSkuMapper.selectBatchIds(
                        configs.stream().map(SeckillSku::getSkuId).toList()).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        vo.setSkus(configs.stream().map(c -> toSkuOption(c, productSkus.get(c.getSkuId())))
                .filter(Objects::nonNull).toList());
        return vo;
    }

    @Override
    public SeckillOrderPreviewVO preview(Long userId, Long merchantId, SeckillOrderPreviewRequest request) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.SECKILL);
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, request.getAddressId())
                .eq(UserAddress::getUserId, userId));
        if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        int quantity = safeQuantity(request.getQuantity());
        SeckillContext context = loadContext(merchantId, request.getSessionId(), request.getSeckillSkuId(), null, true);
        validateCanBuy(userId, context, quantity);
        SeckillOrderPreviewVO vo = new SeckillOrderPreviewVO();
        vo.setAddress(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail()));
        vo.setSessionId(context.session.getId());
        vo.setSeckillSkuId(context.seckillSku.getId());
        vo.setProductName(context.product.getName());
        vo.setMainImage(context.productSku.getImage() == null || context.productSku.getImage().isBlank()
                ? context.product.getMainImage() : context.productSku.getImage());
        vo.setSpecText(context.productSku.getSpecText());
        vo.setActivityPrice(context.seckillSku.getActivityPrice());
        vo.setQuantity(quantity);
        BigDecimal total = context.seckillSku.getActivityPrice().multiply(BigDecimal.valueOf(quantity));
        vo.setTotalAmount(total);
        vo.setFreightAmount(BigDecimal.ZERO);
        vo.setDiscountAmount(BigDecimal.ZERO);
        vo.setPayAmount(total);
        vo.setUserLimit(context.seckillSku.getUserLimit());
        vo.setRemainingStock(Math.min(context.seckillSku.getActivityStock(), context.productSku.getStock()));
        vo.setRuleText("限时秒杀商品不支持使用优惠券，每人限购" + context.seckillSku.getUserLimit() + "件");
        return vo;
    }

    @Override
    public OrderCreateVO createOrder(Long userId, Long merchantId, SeckillOrderCreateRequest request) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.SECKILL);
        int quantity = safeQuantity(request.getQuantity());
        String lockKey = CREATE_LOCK_PREFIX + userId + ":" + request.getSessionId() + ":" + request.getSeckillSkuId();
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 5, TimeUnit.SECONDS);
        if (locked == null || !locked) throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        try {
            Order order = new TransactionTemplate(transactionManager).execute(status -> {
                UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getId, request.getAddressId()).eq(UserAddress::getUserId, userId));
                if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
                SeckillContext context = loadContext(merchantId, request.getSessionId(), request.getSeckillSkuId(), null, true);
                validateCanBuy(userId, context, quantity);
                SeckillSku lockedSku = seckillSkuMapper.selectByIdForUpdate(context.seckillSku.getId());
                if (lockedSku == null) throw new BusinessException(ErrorCode.SECKILL_SOLD_OUT);
                context.seckillSku = lockedSku;
                validateCanBuy(userId, context, quantity);
                if (seckillSkuMapper.reserveStock(lockedSku.getId(), quantity) == 0) {
                    throw new BusinessException(ErrorCode.SECKILL_SOLD_OUT);
                }
                if (productSkuMapper.deductStock(context.productSku.getId(), quantity) == 0) {
                    throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
                }
                String orderNo = generateOrderNo(userId);
                Order created = new Order();
                created.setOrderNo(orderNo);
                created.setUserId(userId);
                created.setMerchantId(merchantId);
                created.setStatus(OrderStatus.WAIT_PAY.getCode());
                created.setOrderType(2);
                created.setSeckillSessionId(context.session.getId());
                created.setSeckillSkuId(lockedSku.getId());
                BigDecimal subtotal = lockedSku.getActivityPrice().multiply(BigDecimal.valueOf(quantity));
                created.setTotalAmount(subtotal);
                created.setFreightAmount(BigDecimal.ZERO);
                created.setDiscountAmount(BigDecimal.ZERO);
                created.setPayAmount(subtotal);
                created.setAddressSnapshot(toJson(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail())));
                created.setRemark(request.getRemark() == null ? "" : request.getRemark().trim());
                orderMapper.insert(created);

                OrderItem item = new OrderItem();
                item.setOrderId(created.getId());
                item.setOrderNo(orderNo);
                item.setProductId(context.product.getId());
                item.setSkuId(context.productSku.getId());
                item.setProductName(context.product.getName());
                item.setMainImage(context.productSku.getImage() == null || context.productSku.getImage().isBlank()
                        ? context.product.getMainImage() : context.productSku.getImage());
                item.setSpecText(context.productSku.getSpecText());
                item.setUnitPrice(lockedSku.getActivityPrice());
                item.setQuantity(quantity);
                item.setSubtotal(subtotal);
                orderItemMapper.insert(item);

                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setOrderNo(orderNo);
                seckillOrder.setMerchantId(merchantId);
                seckillOrder.setActivityId(context.activity.getId());
                seckillOrder.setSessionId(context.session.getId());
                seckillOrder.setSeckillSkuId(lockedSku.getId());
                seckillOrder.setProductId(context.product.getId());
                seckillOrder.setSkuId(context.productSku.getId());
                seckillOrder.setUserId(userId);
                seckillOrder.setQuantity(quantity);
                seckillOrder.setActivityPrice(lockedSku.getActivityPrice());
                seckillOrder.setStatus(0);
                seckillOrder.setStockReleased(0);
                seckillOrderMapper.insert(seckillOrder);
                productService.recalcProduct(context.product.getId());
                return created;
            });
            OrderCreateVO vo = new OrderCreateVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setPayAmount(order.getPayAmount());
            try {
                vo.setPayParams(wxPayService.createJsapiPayParams(order));
            } catch (RuntimeException ex) {
                log.warn("秒杀订单已创建但微信预下单失败，可稍后重新支付, orderNo={}", order.getOrderNo(), ex);
            }
            return vo;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public void handleOrderPaid(String orderNo) {
        SeckillOrder order = seckillOrderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo).last("FOR UPDATE"));
        if (order == null || order.getStatus() != 0) return;
        order.setStatus(1);
        seckillOrderMapper.updateById(order);
        seckillSkuMapper.addSoldCount(order.getSeckillSkuId(), order.getQuantity());
    }

    @Override
    @Transactional
    public void releaseForOrder(String orderNo, String reason) {
        SeckillOrder order = seckillOrderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo).last("FOR UPDATE"));
        if (order == null || order.getStatus() != 0 || Integer.valueOf(1).equals(order.getStockReleased())) return;
        seckillSkuMapper.releaseStock(order.getSeckillSkuId(), order.getQuantity());
        order.setStatus(2);
        order.setStockReleased(1);
        seckillOrderMapper.updateById(order);
        log.info("释放秒杀活动库存, orderNo={}, reason={}", orderNo, reason);
    }

    @Override
    public PageResult<SeckillActivityVO> merchantPage(Long merchantId, int page, int size) {
        Page<SeckillActivity> result = activityMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SeckillActivity>().eq(SeckillActivity::getMerchantId, merchantId)
                        .orderByDesc(SeckillActivity::getId));
        List<SeckillActivityVO> list = result.getRecords().stream().map(a -> buildActivity(a, false)).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public SeckillActivityVO merchantGet(Long merchantId, Long activityId) {
        SeckillActivity activity = mustActivity(merchantId, activityId);
        return buildActivity(activity, true);
    }

    @Override
    @Transactional
    public Long saveActivity(Long merchantId, Long operatorId, SeckillActivitySaveRequest request) {
        validateRequest(merchantId, request);
        SeckillActivity activity = new SeckillActivity();
        activity.setMerchantId(merchantId);
        activity.setName(request.getActivityName().trim());
        activity.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        activity.setPreheatAt(request.getPreheatAt());
        activity.setStatus(1);
        activity.setCreatedBy(operatorId);
        activityMapper.insert(activity);
        saveSessions(merchantId, activity.getId(), request.getSessions());
        evictSessions(merchantId);
        return activity.getId();
    }

    @Override
    @Transactional
    public void updateActivity(Long merchantId, Long operatorId, Long activityId, SeckillActivitySaveRequest request) {
        validateRequest(merchantId, request);
        SeckillActivity activity = mustActivity(merchantId, activityId);
        LocalDateTime firstStart = request.getSessions().stream().map(SeckillActivitySaveRequest.Session::getStartAt)
                .min(LocalDateTime::compareTo).orElseThrow();
        if (!LocalDateTime.now().isBefore(firstStart)) {
            throw new BusinessException(ErrorCode.SECKILL_ACTIVITY_STARTED);
        }
        activity.setName(request.getActivityName().trim());
        activity.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        activity.setPreheatAt(request.getPreheatAt());
        activityMapper.updateById(activity);
        List<Long> sessionIds = sessionMapper.selectList(new LambdaQueryWrapper<SeckillSession>()
                .eq(SeckillSession::getActivityId, activityId)).stream().map(SeckillSession::getId).toList();
        if (!sessionIds.isEmpty()) {
            seckillSkuMapper.delete(new LambdaQueryWrapper<SeckillSku>().in(SeckillSku::getSessionId, sessionIds));
            sessionMapper.delete(new LambdaQueryWrapper<SeckillSession>().in(SeckillSession::getId, sessionIds));
        }
        saveSessions(merchantId, activityId, request.getSessions());
        evictSessions(merchantId);
    }

    private void saveSessions(Long merchantId, Long activityId, List<SeckillActivitySaveRequest.Session> sessions) {
        for (SeckillActivitySaveRequest.Session request : sessions) {
            SeckillSession session = new SeckillSession();
            session.setActivityId(activityId);
            session.setMerchantId(merchantId);
            session.setName(request.getName().trim());
            session.setStartAt(request.getStartAt());
            session.setEndAt(request.getEndAt());
            session.setSort(request.getSort() == null ? 0 : request.getSort());
            sessionMapper.insert(session);
            for (SeckillActivitySaveRequest.Sku requestSku : request.getSkus()) {
                ProductSku productSku = productSkuMapper.selectById(requestSku.getSkuId());
                SeckillSku sku = new SeckillSku();
                sku.setSessionId(session.getId());
                sku.setMerchantId(merchantId);
                sku.setProductId(requestSku.getProductId());
                sku.setSkuId(requestSku.getSkuId());
                sku.setActivityPrice(requestSku.getActivityPrice());
                sku.setActivityStock(requestSku.getActivityStock());
                sku.setSoldCount(0);
                sku.setUserLimit(requestSku.getUserLimit());
                seckillSkuMapper.insert(sku);
            }
        }
    }

    private void validateRequest(Long merchantId, SeckillActivitySaveRequest request) {
        if (request == null || request.getActivityName() == null || request.getActivityName().isBlank()
                || request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new BusinessException(ErrorCode.SECKILL_CONFIG_INVALID);
        }
        List<SeckillActivitySaveRequest.Session> sessions = request.getSessions();
        for (int i = 0; i < sessions.size(); i++) {
            SeckillActivitySaveRequest.Session s = sessions.get(i);
            if (s == null) throw new BusinessException(ErrorCode.SECKILL_CONFIG_INVALID);
            if (s.getName() == null || s.getName().isBlank() || s.getStartAt() == null || s.getEndAt() == null
                    || !s.getStartAt().isBefore(s.getEndAt())) {
                throw new BusinessException(ErrorCode.SECKILL_TIME_INVALID);
            }
            if (request.getPreheatAt() != null && request.getPreheatAt().isAfter(s.getStartAt())) {
                throw new BusinessException(ErrorCode.SECKILL_TIME_INVALID.getCode(), "预热时间不能晚于场次开始时间");
            }
            for (int j = i + 1; j < sessions.size(); j++) {
                if (s.getStartAt().isBefore(sessions.get(j).getEndAt())
                        && sessions.get(j).getStartAt().isBefore(s.getEndAt())) {
                    throw new BusinessException(ErrorCode.SECKILL_TIME_INVALID.getCode(), "秒杀场次时间不能重叠");
                }
            }
            if (s.getSkus() == null || s.getSkus().isEmpty()) throw new BusinessException(ErrorCode.SECKILL_CONFIG_INVALID);
            Set<Long> configuredSkuIds = new HashSet<>();
            for (SeckillActivitySaveRequest.Sku item : s.getSkus()) {
                if (item == null || item.getProductId() == null || item.getSkuId() == null
                        || !configuredSkuIds.add(item.getSkuId()) || item.getActivityStock() == null || item.getActivityStock() <= 0
                        || item.getUserLimit() == null || item.getUserLimit() < 1 || item.getUserLimit() > 99
                        || item.getActivityPrice() == null || item.getActivityPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ErrorCode.SECKILL_CONFIG_INVALID);
                }
                Product product = productMapper.selectById(item.getProductId());
                ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                if (product == null || sku == null || !merchantId.equals(product.getMerchantId())
                        || !product.getId().equals(sku.getProductId()) || !Integer.valueOf(1).equals(sku.getActive())
                        || !Integer.valueOf(1).equals(product.getStatus())
                        || item.getActivityPrice().compareTo(sku.getPrice()) >= 0
                        || item.getActivityStock() > Optional.ofNullable(sku.getStock()).orElse(0)) {
                    throw new BusinessException(ErrorCode.SECKILL_CONFIG_INVALID.getCode(), "商品、价格或活动库存不符合秒杀规则");
                }
            }
        }
    }

    private SeckillContext loadContext(Long merchantId, Long sessionId, Long seckillSkuId, Long productId, boolean requirePublic) {
        SeckillSession session = loadSession(merchantId, sessionId, requirePublic);
        SeckillActivity activity = activityMapper.selectById(session.getActivityId());
        if (seckillSkuId == null && productId == null) throw new BusinessException(ErrorCode.SECKILL_CONFIG_INVALID);
        LambdaQueryWrapper<SeckillSku> skuQuery = new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getSessionId, sessionId);
        if (seckillSkuId != null) skuQuery.eq(SeckillSku::getId, seckillSkuId);
        if (productId != null) skuQuery.eq(SeckillSku::getProductId, productId);
        SeckillSku seckillSku = seckillSkuMapper.selectOne(skuQuery);
        if (seckillSku == null) throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        Product product = productMapper.selectById(seckillSku.getProductId());
        ProductSku productSku = productSkuMapper.selectById(seckillSku.getSkuId());
        if (product == null || productSku == null || !merchantId.equals(product.getMerchantId())
                || !product.getId().equals(productSku.getProductId()) || !Integer.valueOf(1).equals(product.getStatus())
                || !Integer.valueOf(1).equals(productSku.getActive())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        return new SeckillContext(activity, session, seckillSku, product, productSku);
    }

    private SeckillSession loadSession(Long merchantId, Long sessionId, boolean requirePublic) {
        SeckillSession session = sessionMapper.selectById(sessionId);
        SeckillActivity activity = session == null ? null : activityMapper.selectById(session.getActivityId());
        if (session == null || activity == null || !merchantId.equals(session.getMerchantId())
                || !merchantId.equals(activity.getMerchantId()) || (requirePublic && activity.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        return session;
    }

    private void validateCanBuy(Long userId, SeckillContext context, int quantity) {
        int status = sessionStatus(context.session);
        if (status == 0) throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        if (status == 2) throw new BusinessException(ErrorCode.SECKILL_ENDED);
        if (quantity < 1 || quantity > context.seckillSku.getUserLimit()) {
            throw new BusinessException(ErrorCode.SECKILL_ORDER_LIMIT);
        }
        int activityStock = Optional.ofNullable(context.seckillSku.getActivityStock()).orElse(0);
        int productStock = Optional.ofNullable(context.productSku.getStock()).orElse(0);
        if (activityStock < quantity) throw new BusinessException(ErrorCode.SECKILL_SOLD_OUT);
        if (productStock < quantity) throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        int purchased = seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getSessionId, context.session.getId())
                .eq(SeckillOrder::getSeckillSkuId, context.seckillSku.getId())
                .in(SeckillOrder::getStatus, 0, 1)).stream()
                .mapToInt(o -> Optional.ofNullable(o.getQuantity()).orElse(0)).sum();
        if (purchased + quantity > context.seckillSku.getUserLimit()) throw new BusinessException(ErrorCode.SECKILL_ORDER_LIMIT);
    }

    private int safeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 99) throw new BusinessException(ErrorCode.SECKILL_ORDER_LIMIT);
        return quantity;
    }

    private SeckillSessionVO buildSession(SeckillSession session, SeckillActivity activity, boolean includeEnded) {
        SeckillSessionVO vo = new SeckillSessionVO();
        vo.setId(session.getId());
        vo.setActivityId(activity.getId());
        vo.setActivityName(activity.getName());
        vo.setActivityDescription(activity.getDescription());
        vo.setName(session.getName());
        vo.setStartAt(session.getStartAt());
        vo.setEndAt(session.getEndAt());
        vo.setStatus(sessionStatus(session));
        vo.setStatusText(sessionStatusText(vo.getStatus()));
        List<SeckillSku> configs = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getSessionId, session.getId()).orderByAsc(SeckillSku::getId));
        Map<Long, Product> products = productMapper.selectBatchIds(configs.stream().map(SeckillSku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, ProductSku> skus = productSkuMapper.selectBatchIds(configs.stream().map(SeckillSku::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        vo.setProducts(configs.stream().map(c -> toProductVO(c, products.get(c.getProductId()), skus.get(c.getSkuId()), vo.getStatus()))
                .filter(Objects::nonNull).toList());
        return vo;
    }

    private SeckillActivityVO buildActivity(SeckillActivity activity, boolean includeEnded) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setDescription(activity.getDescription());
        vo.setPreheatAt(activity.getPreheatAt());
        vo.setStatus(activity.getStatus());
        vo.setStatusText(activity.getStatus() == 1 ? "已发布" : activity.getStatus() == 2 ? "已停用" : "草稿");
        List<SeckillSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<SeckillSession>()
                .eq(SeckillSession::getActivityId, activity.getId()).orderByAsc(SeckillSession::getSort).orderByAsc(SeckillSession::getStartAt));
        vo.setSessions(sessions.stream().map(s -> buildAdminSession(s)).toList());
        return vo;
    }

    private SeckillAdminSessionVO buildAdminSession(SeckillSession session) {
        SeckillAdminSessionVO vo = new SeckillAdminSessionVO();
        vo.setId(session.getId());
        vo.setName(session.getName());
        vo.setStartAt(session.getStartAt());
        vo.setEndAt(session.getEndAt());
        vo.setSort(session.getSort());
        List<SeckillSku> configs = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getSessionId, session.getId()).orderByAsc(SeckillSku::getId));
        Map<Long, Product> products = productMapper.selectBatchIds(configs.stream().map(SeckillSku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, ProductSku> skus = productSkuMapper.selectBatchIds(configs.stream().map(SeckillSku::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        vo.setSkus(configs.stream().map(config -> {
            SeckillAdminSessionVO.Sku sku = new SeckillAdminSessionVO.Sku();
            Product product = products.get(config.getProductId());
            ProductSku productSku = skus.get(config.getSkuId());
            sku.setProductId(config.getProductId());
            sku.setSkuId(config.getSkuId());
            sku.setActivityPrice(config.getActivityPrice());
            sku.setActivityStock(config.getActivityStock());
            sku.setSoldCount(config.getSoldCount());
            sku.setUserLimit(config.getUserLimit());
            sku.setProductName(product == null ? "" : product.getName());
            sku.setSpecText(productSku == null ? "" : productSku.getSpecText());
            return sku;
        }).toList());
        return vo;
    }

    private SeckillProductVO toProductVO(SeckillSku config, Product product, ProductSku sku, int sessionStatus) {
        if (product == null || sku == null || product.getStatus() == null || product.getStatus() != 1 || sku.getActive() == null || sku.getActive() != 1) return null;
        SeckillProductVO vo = new SeckillProductVO();
        vo.setSeckillSkuId(config.getId());
        vo.setProductId(product.getId());
        vo.setSkuId(sku.getId());
        vo.setProductName(product.getName());
        vo.setMainImage(sku.getImage() == null || sku.getImage().isBlank() ? product.getMainImage() : sku.getImage());
        vo.setSpecText(sku.getSpecText());
        vo.setActivityPrice(config.getActivityPrice());
        vo.setOriginalPrice(sku.getPrice());
        vo.setActivityStock(config.getActivityStock());
        vo.setSoldCount(config.getSoldCount());
        vo.setRemainingStock(Math.min(Optional.ofNullable(config.getActivityStock()).orElse(0), Optional.ofNullable(sku.getStock()).orElse(0)));
        vo.setUserLimit(config.getUserLimit());
        int status = sessionStatus;
        if (status == 1 && vo.getRemainingStock() <= 0) status = 3;
        vo.setStatus(status);
        vo.setStatusText(status == 0 ? "即将开始" : status == 1 ? "立即抢购" : status == 3 ? "已售罄" : "已结束");
        return vo;
    }

    private void fillSelected(SeckillProductDetailVO vo, SeckillSku config, ProductSku sku) {
        vo.setActivityPrice(config.getActivityPrice());
        vo.setOriginalPrice(sku.getPrice());
        vo.setActivityStock(config.getActivityStock());
        vo.setSoldCount(config.getSoldCount());
        vo.setRemainingStock(Math.min(Optional.ofNullable(config.getActivityStock()).orElse(0), Optional.ofNullable(sku.getStock()).orElse(0)));
        vo.setUserLimit(config.getUserLimit());
    }

    private SeckillProductDetailVO.SeckillSkuOptionVO toSkuOption(SeckillSku config, ProductSku sku) {
        if (sku == null) return null;
        SeckillProductDetailVO.SeckillSkuOptionVO vo = new SeckillProductDetailVO.SeckillSkuOptionVO();
        vo.setSkuId(sku.getId());
        vo.setSpecText(sku.getSpecText());
        vo.setActivityPrice(config.getActivityPrice());
        vo.setOriginalPrice(sku.getPrice());
        vo.setSpecValueIds(sku.getSpecValueIds() == null ? List.of() : sku.getSpecValueIds());
        vo.setImage(sku.getImage());
        vo.setStock(sku.getStock());
        vo.setActivityStock(config.getActivityStock());
        vo.setSoldCount(config.getSoldCount());
        vo.setRemainingStock(Math.min(Optional.ofNullable(config.getActivityStock()).orElse(0), Optional.ofNullable(sku.getStock()).orElse(0)));
        vo.setUserLimit(config.getUserLimit());
        return vo;
    }

    private SeckillActivity mustActivity(Long merchantId, Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !merchantId.equals(activity.getMerchantId())) throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        return activity;
    }

    private int sessionStatus(SeckillSession session) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartAt())) return 0;
        if (now.isBefore(session.getEndAt())) return 1;
        return 2;
    }

    private String sessionStatusText(int status) {
        return status == 0 ? "即将开始" : status == 1 ? "抢购中" : "已结束";
    }

    private String generateOrderNo(Long userId) {
        String prefix = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        for (int i = 0; i < 100; i++) {
            String no = prefix + String.format("%04d", userId % 10000) + String.format("%04d", new Random().nextInt(10000));
            if (orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no)) == 0) return no;
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "地址快照生成失败");
        }
    }

    private void evictSessions(Long merchantId) {
        try {
            redisTemplate.delete(SESSION_CACHE_PREFIX + merchantId);
        } catch (RuntimeException ex) {
            log.debug("清理秒杀场次缓存失败 merchantId={}", merchantId, ex);
        }
    }

    private static class SeckillContext {
        private final SeckillActivity activity;
        private final SeckillSession session;
        private SeckillSku seckillSku;
        private final Product product;
        private final ProductSku productSku;

        private SeckillContext(SeckillActivity activity, SeckillSession session, SeckillSku seckillSku,
                               Product product, ProductSku productSku) {
            this.activity = activity;
            this.session = session;
            this.seckillSku = seckillSku;
            this.product = product;
            this.productSku = productSku;
        }
    }
}
