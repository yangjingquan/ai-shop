package com.shop.presale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.dto.AddressSnapshot;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.WxPayService;
import com.shop.presale.dto.*;
import com.shop.presale.entity.PresaleActivity;
import com.shop.presale.entity.PresaleOrder;
import com.shop.presale.entity.PresalePayment;
import com.shop.presale.entity.PresaleSku;
import com.shop.presale.mapper.PresaleActivityMapper;
import com.shop.presale.mapper.PresaleOrderMapper;
import com.shop.presale.mapper.PresalePaymentMapper;
import com.shop.presale.mapper.PresaleSkuMapper;
import com.shop.presale.service.PresaleService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.service.ProductService;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresaleServiceImpl implements PresaleService {
    private static final int DEPOSIT_WAIT_PAY = 0;
    private static final int WAIT_BALANCE = 1;
    private static final int BALANCE_WAIT_PAY = 2;
    private static final int WAIT_SHIP = 3;
    private static final int FINISHED = 4;
    private static final int REFUNDING = 5;
    private static final int OVERDUE_PENDING = 6;
    private static final int CANCELLED = 7;

    private final PresaleActivityMapper activityMapper;
    private final PresaleSkuMapper skuMapper;
    private final PresaleOrderMapper presaleOrderMapper;
    private final PresalePaymentMapper paymentMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final UserAddressMapper addressMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundApplicationMapper refundMapper;
    private final MarketingFeatureService marketingFeatureService;
    private final ProductService productService;
    private final WxPayService wxPayService;

    @Override
    public List<PresaleActivityVO> active(Long merchantId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.PRESALE);
        LocalDateTime now = LocalDateTime.now();
        return activityMapper.selectList(new LambdaQueryWrapper<PresaleActivity>()
                        .eq(PresaleActivity::getMerchantId, merchantId)
                        .eq(PresaleActivity::getStatus, 1)
                        .ge(PresaleActivity::getBalanceEndAt, now)
                        .orderByAsc(PresaleActivity::getDepositStartAt))
                .stream().map(activity -> buildActivity(activity, false)).toList();
    }

    @Override
    public PresaleActivityVO detail(Long merchantId, Long activityId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.PRESALE);
        return buildActivity(mustActivity(merchantId, activityId), true);
    }

    @Override
    public PresaleOrderVO order(Long userId, Long merchantId, String orderNo) {
        PresaleOrder order = presaleOrderMapper.selectByAnyOrderNo(userId, orderNo);
        if (order == null || !Objects.equals(order.getMerchantId(), merchantId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        PresaleActivity activity = activityMapper.selectById(order.getActivityId());
        PresaleOrderVO vo = new PresaleOrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setDepositOrderNo(order.getDepositOrderNo());
        vo.setBalanceOrderNo(order.getBalanceOrderNo());
        vo.setActivityId(order.getActivityId());
        vo.setActivityName(activity == null ? "预售活动" : activity.getName());
        vo.setPresaleSkuId(order.getPresaleSkuId());
        vo.setProductName(order.getProductName());
        vo.setMainImage(order.getMainImage());
        vo.setSpecText(order.getSpecText());
        vo.setQuantity(order.getQuantity());
        vo.setStage(order.getStage());
        vo.setStageText(stageText(order.getStage()));
        vo.setDepositAmount(order.getDepositAmount());
        vo.setDepositDeductionAmount(order.getDepositDeductionAmount());
        vo.setBalanceAmount(order.getBalanceAmount());
        vo.setFinalPrice(order.getFinalPrice());
        vo.setFinalAmount(order.getFinalAmount());
        vo.setDepositPaidAt(order.getDepositPaidAt());
        vo.setBalanceDeadline(order.getBalanceDeadline());
        vo.setBalancePaidAt(order.getBalancePaidAt());
        vo.setExpectedShipAt(order.getExpectedShipAt());
        LocalDateTime now = LocalDateTime.now();
        vo.setCanPayBalance((order.getStage() == WAIT_BALANCE || order.getStage() == BALANCE_WAIT_PAY) && activity != null
                && !now.isBefore(activity.getBalanceStartAt()) && now.isBefore(activity.getBalanceEndAt()));
        vo.setCanRefundDeposit(order.getStage() == WAIT_BALANCE || order.getStage() == OVERDUE_PENDING);
        vo.setCanChangeAddress(order.getStage() <= BALANCE_WAIT_PAY);
        return vo;
    }

    @Override
    @Transactional
    public OrderCreateVO createDepositOrder(Long userId, Long merchantId, PresaleDepositOrderRequest request) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.PRESALE);
        PresaleActivity activity = mustActivity(merchantId, request.getActivityId());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getDepositStartAt())) throw new BusinessException(ErrorCode.PRESALE_NOT_STARTED);
        if (!now.isBefore(activity.getDepositEndAt())) throw new BusinessException(ErrorCode.PRESALE_DEPOSIT_ENDED);

        PresaleSku config = skuMapper.selectForUpdate(request.getPresaleSkuId());
        Product product = productMapper.selectById(request.getPresaleSkuId() == null ? null : config == null ? null : config.getProductId());
        ProductSku productSku = config == null ? null : productSkuMapper.selectById(config.getSkuId());
        validateSku(activity, config, product, productSku);
        int quantity = request.getQuantity();
        if (quantity > config.getUserLimit()) throw new BusinessException(ErrorCode.PRESALE_CONFIG_INVALID.getCode(), "超过每人限购数量");
        int bought = presaleOrderMapper.selectActiveByUserSku(userId, activity.getId(), config.getId()).stream()
                .mapToInt(item -> Optional.ofNullable(item.getQuantity()).orElse(0)).sum();
        if (bought + quantity > config.getUserLimit()) throw new BusinessException(ErrorCode.PRESALE_CONFIG_INVALID.getCode(), "超过每人限购数量");
        UserAddress address = mustAddress(userId, request.getAddressId());

        BigDecimal deposit = config.getDepositAmount().multiply(BigDecimal.valueOf(quantity));
        BigDecimal deduction = config.getDepositDeductionAmount().multiply(BigDecimal.valueOf(quantity));
        BigDecimal balance = config.getFinalPrice().subtract(config.getDepositDeductionAmount()).multiply(BigDecimal.valueOf(quantity));
        BigDecimal finalAmount = deposit.add(balance);
        String orderNo = generateOrderNo();

        PresaleOrder presale = new PresaleOrder();
        presale.setOrderNo(orderNo);
        presale.setDepositOrderNo(orderNo);
        presale.setMerchantId(merchantId);
        presale.setActivityId(activity.getId());
        presale.setPresaleSkuId(config.getId());
        presale.setProductId(product.getId());
        presale.setSkuId(productSku.getId());
        presale.setUserId(userId);
        presale.setQuantity(quantity);
        presale.setStage(DEPOSIT_WAIT_PAY);
        presale.setProductName(product.getName());
        presale.setMainImage(product.getMainImage());
        presale.setSpecText(productSku.getSpecText());
        presale.setDepositAmount(deposit);
        presale.setDepositDeductionAmount(deduction);
        presale.setBalanceAmount(balance);
        presale.setFinalPrice(config.getFinalPrice().multiply(BigDecimal.valueOf(quantity)));
        presale.setFinalAmount(finalAmount);
        presale.setExpectedShipAt(activity.getExpectedShipAt());
        presaleOrderMapper.insert(presale);

        AddressSnapshot addressSnapshot = new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail());
        Order order = paymentOrder(presale, orderNo, 1, deposit, addressSnapshot);
        orderMapper.insert(order);
        insertItem(order, product, productSku, config.getDepositAmount(), quantity);
        return payVO(order);
    }

    @Override
    @Transactional
    public OrderCreateVO createBalanceOrder(Long userId, Long merchantId, String orderNo) {
        PresaleOrder presale = presaleOrderMapper.selectByAnyOrderNo(userId, orderNo);
        if (presale == null || !merchantId.equals(presale.getMerchantId())) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        PresaleActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<PresaleActivity>()
                .eq(PresaleActivity::getId, presale.getActivityId())
                .eq(PresaleActivity::getMerchantId, merchantId));
        if (activity == null) throw new BusinessException(ErrorCode.PRESALE_ORDER_INVALID);
        LocalDateTime now = LocalDateTime.now();
        if (presale.getStage() != WAIT_BALANCE && presale.getStage() != BALANCE_WAIT_PAY) throw new BusinessException(ErrorCode.PRESALE_ORDER_INVALID);
        if (now.isBefore(activity.getBalanceStartAt())) throw new BusinessException(ErrorCode.PRESALE_BALANCE_NOT_STARTED);
        if (!now.isBefore(activity.getBalanceEndAt())) throw new BusinessException(ErrorCode.PRESALE_BALANCE_ENDED);

        if (presale.getBalanceOrderNo() != null && !presale.getBalanceOrderNo().isBlank()) {
            Order existing = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, presale.getBalanceOrderNo()));
            if (existing != null && existing.getStatus() == OrderStatus.WAIT_PAY.getCode()) return payVO(existing);
        }
        Order depositOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, presale.getDepositOrderNo()));
        if (depositOrder == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        String balanceOrderNo = generateOrderNo();
        presale.setBalanceOrderNo(balanceOrderNo);
        presale.setStage(BALANCE_WAIT_PAY);
        presale.setBalanceDeadline(activity.getBalanceEndAt());
        presaleOrderMapper.updateById(presale);

        AddressSnapshot address = readAddress(depositOrder.getAddressSnapshot());
        Order order = paymentOrder(presale, balanceOrderNo, 2, presale.getBalanceAmount(), address);
        order.setUserDeleted(1);
        orderMapper.insert(order);
        Product product = productMapper.selectById(presale.getProductId());
        ProductSku productSku = productSkuMapper.selectById(presale.getSkuId());
        insertItem(order, product, productSku, presale.getBalanceAmount().divide(BigDecimal.valueOf(presale.getQuantity())), presale.getQuantity());
        return payVO(order);
    }

    @Override
    @Transactional
    public void handleOrderPaid(String orderNo, String transactionId, String rawPayload) {
        PresaleOrder presale = presaleOrderMapper.selectByAnyOrderNoForUpdate(orderNo);
        if (presale == null) return;
        if (paymentMapper.selectByTransactionId(transactionId) != null) return;
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("FOR UPDATE"));
        if (order == null) return;
        int paymentStage = Objects.equals(orderNo, presale.getDepositOrderNo()) ? 1 : 2;
        PresalePayment payment = new PresalePayment();
        payment.setPresaleOrderId(presale.getId());
        payment.setOrderNo(orderNo);
        payment.setPaymentStage(paymentStage);
        payment.setAmount(order.getPayAmount());
        payment.setTransactionId(transactionId);
        payment.setStatus(1);
        try {
            paymentMapper.insert(payment);
        } catch (DuplicateKeyException duplicate) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPayTime(now);
        order.setPayTransactionId(transactionId);
        order.setPayMethod(1);
        if (paymentStage == 1 && presale.getStage() == DEPOSIT_WAIT_PAY) {
            presale.setStage(WAIT_BALANCE);
            presale.setDepositPaidAt(now);
            PresaleActivity activity = activityMapper.selectById(presale.getActivityId());
            if (activity != null) presale.setBalanceDeadline(activity.getBalanceEndAt());
            order.setStatus(OrderStatus.PRESALE_WAIT_BALANCE.getCode());
            orderMapper.updateById(order);
            presaleOrderMapper.updateById(presale);
            skuMapper.addDepositCount(presale.getPresaleSkuId(), presale.getQuantity());
            return;
        }
        if (paymentStage == 2 && presale.getStage() == BALANCE_WAIT_PAY) {
            int affected = productSkuMapper.deductStock(presale.getSkuId(), presale.getQuantity());
            if (affected == 0) {
                order.setStatus(OrderStatus.CANCELLED.getCode());
                order.setCancelReason("PRESALE_STOCK_REFUND");
                orderMapper.updateById(order);
                presale.setStage(REFUNDING);
                presaleOrderMapper.updateById(presale);
                createAutoRefund(order, "尾款支付成功但库存不足，系统自动退款");
                return;
            }
            order.setStatus(OrderStatus.WAIT_SHIP.getCode());
            orderMapper.updateById(order);
            presale.setStage(WAIT_SHIP);
            presale.setBalancePaidAt(now);
            presaleOrderMapper.updateById(presale);
            skuMapper.addBalanceSoldCount(presale.getPresaleSkuId(), presale.getQuantity());
            orderMapper.addTotalSales(presale.getProductId(), presale.getQuantity());
            productService.recalcProduct(presale.getProductId());
        }
    }

    @Override
    @Transactional
    public void syncShipping(Long merchantId, String balanceOrderNo) {
        PresaleOrder presale = presaleOrderMapper.selectByAnyOrderNoForUpdate(balanceOrderNo);
        if (presale == null || !merchantId.equals(presale.getMerchantId()) || !balanceOrderNo.equals(presale.getBalanceOrderNo())) return;
        Order balanceOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, balanceOrderNo));
        Order depositOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, presale.getDepositOrderNo()));
        if (balanceOrder == null || depositOrder == null) return;
        depositOrder.setStatus(OrderStatus.WAIT_RECEIVE.getCode());
        depositOrder.setShipCompany(balanceOrder.getShipCompany());
        depositOrder.setShipperCode(balanceOrder.getShipperCode());
        depositOrder.setShipNo(balanceOrder.getShipNo());
        depositOrder.setShipTime(balanceOrder.getShipTime());
        orderMapper.updateById(depositOrder);
    }

    @Override
    @Transactional
    public void syncFinished(Long userId, String orderNo) {
        PresaleOrder presale = presaleOrderMapper.selectByAnyOrderNo(userId, orderNo);
        if (presale == null || presale.getStage() != WAIT_SHIP) return;
        presale.setStage(FINISHED);
        presaleOrderMapper.updateById(presale);
        if (presale.getBalanceOrderNo() != null) {
            Order balanceOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, presale.getBalanceOrderNo()));
            if (balanceOrder != null && balanceOrder.getStatus() == OrderStatus.WAIT_RECEIVE.getCode()) {
                balanceOrder.setStatus(OrderStatus.FINISHED.getCode());
                balanceOrder.setFinishTime(LocalDateTime.now());
                orderMapper.updateById(balanceOrder);
            }
        }
    }

    @Override
    public String afterSalesOrderNo(Long userId, String orderNo) {
        PresaleOrder presale = presaleOrderMapper.selectByAnyOrderNo(userId, orderNo);
        if (presale != null && presale.getStage() >= WAIT_SHIP && presale.getStage() <= FINISHED
                && presale.getBalanceOrderNo() != null) return presale.getBalanceOrderNo();
        return orderNo;
    }

    @Override
    public void assertPayable(Long userId, Long merchantId, String orderNo) {
        PresaleOrder presale = presaleOrderMapper.selectByAnyOrderNo(userId, orderNo);
        if (presale == null || !merchantId.equals(presale.getMerchantId())) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        if (presale.getStage() != DEPOSIT_WAIT_PAY && presale.getStage() != BALANCE_WAIT_PAY) {
            throw new BusinessException(ErrorCode.ORDER_NOT_REPAYABLE);
        }
        PresaleActivity activity = activityMapper.selectById(presale.getActivityId());
        LocalDateTime now = LocalDateTime.now();
        if (activity == null) throw new BusinessException(ErrorCode.ORDER_NOT_REPAYABLE);
        if (presale.getStage() == DEPOSIT_WAIT_PAY && (now.isBefore(activity.getDepositStartAt()) || !now.isBefore(activity.getDepositEndAt()))) {
            throw new BusinessException(ErrorCode.PRESALE_DEPOSIT_ENDED);
        }
        if (presale.getStage() == BALANCE_WAIT_PAY && (now.isBefore(activity.getBalanceStartAt()) || !now.isBefore(activity.getBalanceEndAt()))) {
            throw new BusinessException(ErrorCode.PRESALE_BALANCE_ENDED);
        }
    }

    @Override
    @Transactional
    public void refundDeposit(Long userId, Long merchantId, String orderNo) {
        PresaleOrder order = ownedOrder(userId, merchantId, orderNo);
        if (order.getStage() != WAIT_BALANCE && order.getStage() != OVERDUE_PENDING) {
            throw new BusinessException(ErrorCode.PRESALE_ORDER_INVALID);
        }
        Order depositOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, order.getDepositOrderNo()).last("FOR UPDATE"));
        if (depositOrder == null || depositOrder.getPayTime() == null) throw new BusinessException(ErrorCode.PRESALE_ORDER_INVALID);
        createAutoRefund(depositOrder, "用户申请退还预售定金");
        order.setStage(REFUNDING);
        presaleOrderMapper.updateById(order);
        depositOrder.setStatus(OrderStatus.CANCELLED.getCode());
        depositOrder.setCancelReason("PRESALE_REFUND");
        orderMapper.updateById(depositOrder);
    }

    @Override
    @Transactional
    public void cancelOrRefundDeposit(Long userId, Long merchantId, String orderNo) {
        PresaleOrder order = ownedOrder(userId, merchantId, orderNo);
        if (order.getStage() == DEPOSIT_WAIT_PAY) {
            Order base = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, order.getDepositOrderNo()).last("FOR UPDATE"));
            if (base != null) {
                base.setStatus(OrderStatus.CANCELLED.getCode());
                base.setCancelReason("USER_CANCEL");
                orderMapper.updateById(base);
            }
            order.setStage(CANCELLED);
            presaleOrderMapper.updateById(order);
            return;
        }
        refundDeposit(userId, merchantId, orderNo);
    }

    @Override
    @Transactional
    public void updateAddress(Long userId, Long merchantId, String orderNo, Long addressId) {
        PresaleOrder presale = ownedOrder(userId, merchantId, orderNo);
        if (presale.getStage() > BALANCE_WAIT_PAY) throw new BusinessException(ErrorCode.PRESALE_ORDER_INVALID);
        UserAddress address = mustAddress(userId, addressId);
        String json = toJson(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail()));
        updateAddressSnapshot(presale.getDepositOrderNo(), json);
        if (presale.getBalanceOrderNo() != null) updateAddressSnapshot(presale.getBalanceOrderNo(), json);
    }

    @Override
    @Transactional
    public int expireBalanceOrders(int limit) {
        int count = 0;
        for (PresaleOrder item : presaleOrderMapper.selectExpiredUnpaidDeposit(limit)) {
            PresaleOrder order = presaleOrderMapper.selectByAnyOrderNoForUpdate(item.getOrderNo());
            if (order == null || order.getStage() != DEPOSIT_WAIT_PAY) continue;
            Order base = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, order.getDepositOrderNo()).last("FOR UPDATE"));
            if (base != null && base.getStatus() == OrderStatus.WAIT_PAY.getCode()) {
                base.setStatus(OrderStatus.CANCELLED.getCode());
                base.setCancelReason("TIMEOUT");
                base.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(base);
            }
            order.setStage(CANCELLED);
            presaleOrderMapper.updateById(order);
            count++;
        }
        for (PresaleOrder item : presaleOrderMapper.selectExpiredWaitingBalance(limit)) {
            PresaleOrder order = presaleOrderMapper.selectByAnyOrderNoForUpdate(item.getOrderNo());
            if (order == null || order.getStage() != WAIT_BALANCE) continue;
            PresaleActivity activity = activityMapper.selectById(order.getActivityId());
            if (activity != null && Integer.valueOf(1).equals(activity.getAutoCloseExpired())) {
                refundDeposit(order.getUserId(), order.getMerchantId(), order.getOrderNo());
            } else {
                order.setStage(OVERDUE_PENDING);
                presaleOrderMapper.updateById(order);
                updateBaseStatus(order.getDepositOrderNo(), OrderStatus.PRESALE_OVERDUE.getCode(), "PRESALE_BALANCE_EXPIRED");
            }
            count++;
        }
        return count;
    }

    @Override
    public PageResult<PresaleActivityVO> merchantPage(Long merchantId, int page, int size) {
        IPage<PresaleActivity> result = activityMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<PresaleActivity>()
                .eq(PresaleActivity::getMerchantId, merchantId).orderByDesc(PresaleActivity::getId));
        return PageResult.of(result.getRecords().stream().map(item -> buildActivity(item, true)).toList(), result.getTotal(), page, size);
    }

    @Override
    public PresaleActivityVO merchantGet(Long merchantId, Long activityId) {
        return buildActivity(mustActivity(merchantId, activityId), true);
    }

    @Override
    @Transactional
    public Long saveActivity(Long merchantId, Long operatorId, PresaleActivitySaveRequest request) {
        validateRequest(merchantId, null, request);
        PresaleActivity activity = new PresaleActivity();
        activity.setMerchantId(merchantId);
        activity.setName(request.getActivityName().trim());
        activity.setDescription(blank(request.getDescription()));
        activity.setBannerImage(blank(request.getBannerImage()));
        copyActivityFields(activity, request);
        activity.setStatus(1);
        activity.setCreatedBy(operatorId);
        activityMapper.insert(activity);
        saveSkus(merchantId, activity.getId(), request.getSkus());
        return activity.getId();
    }

    @Override
    @Transactional
    public void updateActivity(Long merchantId, Long operatorId, Long activityId, PresaleActivitySaveRequest request) {
        PresaleActivity activity = mustActivity(merchantId, activityId);
        if (activity.getDepositStartAt().isBefore(LocalDateTime.now())) throw new BusinessException(ErrorCode.PRESALE_ACTIVITY_STARTED);
        validateRequest(merchantId, activityId, request);
        activity.setName(request.getActivityName().trim());
        activity.setDescription(blank(request.getDescription()));
        activity.setBannerImage(blank(request.getBannerImage()));
        copyActivityFields(activity, request);
        activityMapper.updateById(activity);
        skuMapper.delete(new LambdaQueryWrapper<PresaleSku>().eq(PresaleSku::getActivityId, activityId));
        saveSkus(merchantId, activityId, request.getSkus());
    }

    @Override
    public PresaleStatsVO stats(Long merchantId, Long activityId) {
        PresaleActivity activity = mustActivity(merchantId, activityId);
        List<PresaleOrder> orders = presaleOrderMapper.selectList(new LambdaQueryWrapper<PresaleOrder>().eq(PresaleOrder::getActivityId, activity.getId()));
        PresaleStatsVO vo = new PresaleStatsVO();
        vo.setDepositOrderCount(orders.stream().filter(item -> item.getDepositPaidAt() != null).count());
        vo.setBalancePaidOrderCount(orders.stream().filter(item -> item.getBalancePaidAt() != null).count());
        vo.setOverdueOrderCount(orders.stream().filter(item -> item.getStage() == OVERDUE_PENDING).count());
        vo.setRefundOrderCount(orders.stream().filter(item -> item.getStage() == REFUNDING).count());
        vo.setFinalAmount(orders.stream().filter(item -> item.getStage() >= WAIT_SHIP && item.getStage() <= FINISHED)
                .map(PresaleOrder::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        return vo;
    }

    private void validateRequest(Long merchantId, Long activityId, PresaleActivitySaveRequest request) {
        if (request.getDepositStartAt() == null || request.getDepositEndAt() == null || request.getBalanceStartAt() == null
                || request.getBalanceEndAt() == null || request.getExpectedShipAt() == null
                || !request.getDepositStartAt().isBefore(request.getDepositEndAt())
                || request.getDepositEndAt().compareTo(request.getBalanceStartAt()) >= 0
                || !request.getBalanceStartAt().isBefore(request.getBalanceEndAt())
                || request.getExpectedShipAt().isBefore(request.getBalanceEndAt())
                || request.getSkus() == null || request.getSkus().isEmpty()) {
            throw new BusinessException(ErrorCode.PRESALE_CONFIG_INVALID);
        }
        Set<Long> requestSkuIds = new HashSet<>();
        for (PresaleSkuSaveRequest item : request.getSkus()) {
            if (item.getSkuId() == null || !requestSkuIds.add(item.getSkuId())) {
                throw new BusinessException(ErrorCode.PRESALE_CONFIG_INVALID.getCode(), "同一活动不能重复绑定 SKU");
            }
            Product product = productMapper.selectById(item.getProductId());
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            if (product == null || sku == null || !merchantId.equals(product.getMerchantId()) || !product.getId().equals(sku.getProductId())
                    || !Integer.valueOf(1).equals(product.getStatus()) || !Integer.valueOf(1).equals(sku.getActive())
                    || item.getUserLimit() == null || item.getUserLimit() < 1
                    || item.getFinalPrice() == null || item.getFinalPrice().compareTo(item.getDepositDeductionAmount()) <= 0) {
                throw new BusinessException(ErrorCode.PRESALE_CONFIG_INVALID);
            }
            LambdaQueryWrapper<PresaleSku> q = new LambdaQueryWrapper<PresaleSku>()
                    .eq(PresaleSku::getMerchantId, merchantId).eq(PresaleSku::getSkuId, item.getSkuId());
            if (activityId != null) q.ne(PresaleSku::getActivityId, activityId);
            for (PresaleSku existing : skuMapper.selectList(q)) {
                PresaleActivity other = activityMapper.selectById(existing.getActivityId());
                if (other != null && other.getStatus() == 1 && overlaps(request.getDepositStartAt(), request.getBalanceEndAt(), other.getDepositStartAt(), other.getBalanceEndAt())) {
                    throw new BusinessException(ErrorCode.PRESALE_CONFIG_INVALID.getCode(), "同一 SKU 已存在有效预售活动");
                }
            }
        }
    }

    private void saveSkus(Long merchantId, Long activityId, List<PresaleSkuSaveRequest> requests) {
        for (PresaleSkuSaveRequest item : requests) {
            Product product = productMapper.selectById(item.getProductId());
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            PresaleSku config = new PresaleSku();
            config.setActivityId(activityId); config.setMerchantId(merchantId); config.setProductId(item.getProductId()); config.setSkuId(item.getSkuId());
            config.setDepositAmount(item.getDepositAmount()); config.setDepositDeductionAmount(item.getDepositDeductionAmount());
            config.setFinalPrice(item.getFinalPrice()); config.setBalanceAmount(item.getFinalPrice().subtract(item.getDepositDeductionAmount())); config.setUserLimit(item.getUserLimit()); config.setDepositCount(0); config.setBalanceSoldCount(0);
            skuMapper.insert(config);
        }
    }

    private PresaleActivityVO buildActivity(PresaleActivity activity, boolean includeEnded) {
        PresaleActivityVO vo = new PresaleActivityVO();
        vo.setId(activity.getId()); vo.setName(activity.getName()); vo.setDescription(activity.getDescription()); vo.setBannerImage(activity.getBannerImage());
        vo.setDepositStartAt(activity.getDepositStartAt()); vo.setDepositEndAt(activity.getDepositEndAt());
        vo.setBalanceStartAt(activity.getBalanceStartAt()); vo.setBalanceEndAt(activity.getBalanceEndAt()); vo.setExpectedShipAt(activity.getExpectedShipAt());
        vo.setStatus(activity.getStatus()); vo.setStatusText(activity.getStatus() == 1 ? "已发布" : activity.getStatus() == 2 ? "已停用" : "草稿");
        vo.setAutoCloseExpired(activity.getAutoCloseExpired()); vo.setDepositRefundRule(activity.getDepositRefundRule()); vo.setMerchantBreachRule(activity.getMerchantBreachRule());
        int phase = phase(activity, LocalDateTime.now()); vo.setPhase(phase); vo.setPhaseText(phaseText(phase));
        Map<Long, Product> products = productMapper.selectBatchIds(skuMapper.selectList(new LambdaQueryWrapper<PresaleSku>().eq(PresaleSku::getActivityId, activity.getId())).stream().map(PresaleSku::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, item -> item));
        List<PresaleSku> configs = skuMapper.selectList(new LambdaQueryWrapper<PresaleSku>().eq(PresaleSku::getActivityId, activity.getId()).orderByAsc(PresaleSku::getId));
        Map<Long, ProductSku> productSkus = productSkuMapper.selectBatchIds(configs.stream().map(PresaleSku::getSkuId).distinct().toList()).stream().collect(Collectors.toMap(ProductSku::getId, item -> item));
        for (PresaleSku config : configs) {
            Product product = products.get(config.getProductId()); ProductSku sku = productSkus.get(config.getSkuId());
            if (product == null || sku == null) continue;
            PresaleSkuVO item = new PresaleSkuVO(); item.setId(config.getId()); item.setProductId(product.getId()); item.setSkuId(sku.getId()); item.setProductName(product.getName()); item.setMainImage(product.getMainImage()); item.setSpecText(sku.getSpecText()); item.setOriginalPrice(sku.getPrice()); item.setFinalPrice(config.getFinalPrice());
            item.setDepositAmount(config.getDepositAmount()); item.setDepositDeductionAmount(config.getDepositDeductionAmount()); item.setBalanceAmount(config.getBalanceAmount()); item.setFinalAmount(config.getDepositAmount().add(config.getBalanceAmount())); item.setUserLimit(config.getUserLimit()); item.setDepositCount(config.getDepositCount()); item.setBalanceSoldCount(config.getBalanceSoldCount()); item.setStock(sku.getStock()); vo.getSkus().add(item);
        }
        return vo;
    }

    private PresaleActivity mustActivity(Long merchantId, Long id) {
        PresaleActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<PresaleActivity>().eq(PresaleActivity::getId, id).eq(PresaleActivity::getMerchantId, merchantId));
        if (activity == null || activity.getStatus() != 1) throw new BusinessException(ErrorCode.PRESALE_NOT_FOUND);
        return activity;
    }

    private void validateSku(PresaleActivity activity, PresaleSku config, Product product, ProductSku sku) {
        if (config == null || !activity.getId().equals(config.getActivityId()) || !activity.getMerchantId().equals(config.getMerchantId()) || product == null || sku == null
                || !product.getId().equals(sku.getProductId()) || !activity.getMerchantId().equals(product.getMerchantId()) || product.getStatus() != 1 || sku.getActive() != 1) {
            throw new BusinessException(ErrorCode.PRESALE_NOT_FOUND);
        }
    }

    private UserAddress mustAddress(Long userId, Long addressId) { UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getId, addressId).eq(UserAddress::getUserId, userId)); if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND); return address; }
    private PresaleOrder ownedOrder(Long userId, Long merchantId, String orderNo) { PresaleOrder order = presaleOrderMapper.selectByAnyOrderNo(userId, orderNo); if (order == null || !merchantId.equals(order.getMerchantId())) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND); return order; }
    private void updateAddressSnapshot(String orderNo, String json) { if (orderNo == null) return; Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)); if (order != null) { order.setAddressSnapshot(json); orderMapper.updateById(order); } }
    private void updateBaseStatus(String orderNo, int status, String reason) { Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)); if (order != null) { order.setStatus(status); order.setCancelReason(reason); orderMapper.updateById(order); } }
    private void insertItem(Order order, Product product, ProductSku sku, BigDecimal unitPrice, int quantity) { OrderItem item = new OrderItem(); item.setOrderId(order.getId()); item.setOrderNo(order.getOrderNo()); item.setProductId(product.getId()); item.setSkuId(sku.getId()); item.setProductName(product.getName()); item.setMainImage(product.getMainImage()); item.setSpecText(sku.getSpecText()); item.setUnitPrice(unitPrice); item.setQuantity(quantity); item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity))); orderItemMapper.insert(item); }
    private Order paymentOrder(PresaleOrder presale, String orderNo, int stage, BigDecimal amount, AddressSnapshot address) { Order order = new Order(); order.setOrderNo(orderNo); order.setUserId(presale.getUserId()); order.setMerchantId(presale.getMerchantId()); order.setStatus(OrderStatus.WAIT_PAY.getCode()); order.setOrderType(6); order.setPresaleOrderId(presale.getId()); order.setPresaleStage(stage); order.setTotalAmount(amount); order.setFreightAmount(BigDecimal.ZERO); order.setDiscountAmount(BigDecimal.ZERO); order.setPayAmount(amount); order.setAddressSnapshot(toJson(address)); order.setRemark(""); order.setUserDeleted(0); return order; }
    private OrderCreateVO payVO(Order order) { OrderCreateVO vo = new OrderCreateVO(); vo.setOrderNo(order.getOrderNo()); vo.setPayAmount(order.getPayAmount()); try { vo.setPayParams(wxPayService.createJsapiPayParams(order)); } catch (RuntimeException ex) { log.warn("预售微信预下单失败，可稍后重新支付 orderNo={}", order.getOrderNo(), ex); } return vo; }
    private void createAutoRefund(Order order, String reason) { RefundApplication existing = refundMapper.selectOne(new LambdaQueryWrapper<RefundApplication>().eq(RefundApplication::getOrderNo, order.getOrderNo()).in(RefundApplication::getStatus, 0, 1, 3).orderByDesc(RefundApplication::getId).last("LIMIT 1")); if (existing != null) return; RefundApplication refund = new RefundApplication(); refund.setOrderNo(order.getOrderNo()); refund.setOutRefundNo("RF_PRESALE_" + order.getOrderNo()); refund.setUserId(order.getUserId()); refund.setMerchantId(order.getMerchantId()); refund.setReason(reason); refund.setStatus(RefundStatus.PENDING.getCode()); refund.setRefundAmount(order.getPayAmount()); refund.setAutoRefund(1); refund.setReturnRequired(0); refund.setEvidenceUrls(List.of()); refundMapper.insert(refund); }
    private String generateOrderNo() { return "PS" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss")) + UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
    private String toJson(Object value) { try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("预售地址快照序列化失败", e); } }
    private AddressSnapshot readAddress(String json) { try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, AddressSnapshot.class); } catch (Exception e) { return new AddressSnapshot("", "", "", ""); } }
    private void copyActivityFields(PresaleActivity a, PresaleActivitySaveRequest r) { a.setDepositStartAt(r.getDepositStartAt()); a.setDepositEndAt(r.getDepositEndAt()); a.setBalanceStartAt(r.getBalanceStartAt()); a.setBalanceEndAt(r.getBalanceEndAt()); a.setExpectedShipAt(r.getExpectedShipAt()); a.setAutoCloseExpired(r.getAutoCloseExpired() == null ? 1 : r.getAutoCloseExpired()); a.setDepositRefundRule(blank(r.getDepositRefundRule())); a.setMerchantBreachRule(blank(r.getMerchantBreachRule())); }
    private String blank(String s) { return s == null ? "" : s.trim(); }
    private boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) { return aStart.isBefore(bEnd) && bStart.isBefore(aEnd); }
    private int phase(PresaleActivity a, LocalDateTime now) { if (now.isBefore(a.getDepositStartAt())) return 0; if (now.isBefore(a.getDepositEndAt())) return 1; if (now.isBefore(a.getBalanceStartAt())) return 2; if (now.isBefore(a.getBalanceEndAt())) return 3; return 4; }
    private String phaseText(int p) { return switch (p) { case 0 -> "即将开始"; case 1 -> "支付定金"; case 2 -> "等待尾款"; case 3 -> "支付尾款"; default -> "活动已结束"; }; }
    private String stageText(Integer stage) { return switch (stage == null ? -1 : stage) { case 0 -> "待支付定金"; case 1 -> "待付尾款"; case 2 -> "尾款待支付"; case 3 -> "待发货"; case 4 -> "已完成"; case 5 -> "退款处理中"; case 6 -> "尾款逾期待处理"; case 7 -> "已取消"; default -> "预售订单"; }; }
}
