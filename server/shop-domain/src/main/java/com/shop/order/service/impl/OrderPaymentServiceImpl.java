package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.groupbuy.service.GroupBuyService;
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
import com.shop.order.service.OrderPaymentService;
import com.shop.product.service.ProductService;
import com.shop.seckill.service.SeckillService;
import com.shop.referral.service.ReferralService;
import com.shop.points.service.PointsMemberService;
import com.shop.coupon.service.CouponIssueService;
import com.shop.marketing.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final ProductService productService;
    private final GroupBuyService groupBuyService;
    /** 可选注入，保持支付回调纯单测构造器兼容。 */
    @Autowired(required = false)
    private SeckillService seckillService;
    @Autowired(required = false)
    private ReferralService referralService;
    @Autowired(required = false)
    private PointsMemberService pointsMemberService;
    @Autowired(required = false)
    private CouponIssueService couponIssueService;
    @Autowired(required = false)
    private PromotionService promotionService;
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaidCallback(String orderNo, String transactionId, String rawPayload) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED);
        }
        // MySQL row lock and payment_log unique keys provide durable idempotency. A short Redis lock can expire
        // during processing and is therefore not a correctness boundary for a money flow.
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
            if (order.getStatus() == OrderStatus.CANCELLED.getCode()
                    && ("TIMEOUT".equals(order.getCancelReason())
                    || "USER_CANCEL".equals(order.getCancelReason())
                    || "GROUP_TIMEOUT".equals(order.getCancelReason())
                    || "ADMIN_CANCEL".equals(order.getCancelReason()))) {
                recordLatePaymentAndRefund(order, transactionId, rawPayload);
            }
            return;
        }
        if (!recordPaymentLog(order, transactionId, rawPayload)) {
            return;
        }

        order.setPayTime(LocalDateTime.now());
        order.setPayTransactionId(transactionId);
        order.setPayMethod(1);
        if (Integer.valueOf(1).equals(order.getOrderType())) {
            order.setStatus(OrderStatus.WAIT_GROUP.getCode());
            orderMapper.updateById(order);
            groupBuyService.handleOrderPaid(orderNo);
        } else {
            order.setStatus(OrderStatus.WAIT_SHIP.getCode());
            orderMapper.updateById(order);
            if (Integer.valueOf(2).equals(order.getOrderType()) && seckillService != null) {
                seckillService.handleOrderPaid(orderNo);
            }
        }
        if (referralService != null) referralService.handleOrderPaid(order);
        if (pointsMemberService != null) pointsMemberService.rewardPaidOrder(order);
        if (couponIssueService != null) {
            try {
                couponIssueService.issueAfterPaid(order);
            } catch (RuntimeException ex) {
                // 优惠券奖励不可阻断已验证的支付成功；后续由支付补单/人工记录排查。
                log.error("购后复购券发放失败, orderNo={}", orderNo, ex);
            }
        }
        if (promotionService != null) promotionService.markPaid(orderNo);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        Map<Long, Integer> salesByProduct = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId,
                        Collectors.summingInt(OrderItem::getQuantity)));
        for (Map.Entry<Long, Integer> e : salesByProduct.entrySet()) {
            orderMapper.addTotalSales(e.getKey(), e.getValue());
            productService.recalcProduct(e.getKey());
        }
    }

    private boolean recordPaymentLog(Order order, String transactionId, String rawPayload) {
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setOrderNo(order.getOrderNo());
        paymentLog.setTransactionId(transactionId);
        paymentLog.setAmount(order.getPayAmount());
        paymentLog.setRawPayload(rawPayload);
        try {
            paymentLogMapper.insert(paymentLog);
            return true;
        } catch (DuplicateKeyException e) {
            PaymentLog existing = paymentLogMapper.selectOne(new LambdaQueryWrapper<PaymentLog>()
                    .eq(PaymentLog::getTransactionId, transactionId));
            if (existing != null && !order.getOrderNo().equals(existing.getOrderNo())) {
                throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "微信交易号已关联其他订单");
            }
            return false;
        }
    }

    /**
     * 订单关闭和微信支付成功之间存在竞态窗口。不能因为本地订单已取消就丢弃支付回调，
     * 而应先记录支付流水，再交给退款对账任务自动原路退款。
     */
    private void recordLatePaymentAndRefund(Order order, String transactionId, String rawPayload) {
        if (!recordPaymentLog(order, transactionId, rawPayload)) {
            return;
        }
        RefundApplication refund = new RefundApplication();
        refund.setOrderNo(order.getOrderNo());
        refund.setOutRefundNo("RF_LATE_" + UUID.randomUUID().toString().replace("-", ""));
        refund.setUserId(order.getUserId());
        refund.setMerchantId(order.getMerchantId());
        refund.setReason("支付已完成但订单已关闭，系统自动退款");
        refund.setStatus(RefundStatus.PENDING.getCode());
        refund.setRefundAmount(order.getPayAmount());
        refund.setAutoRefund(1);
        refund.setReturnRequired(0);
        refund.setEvidenceUrls(List.of());
        refundApplicationMapper.insert(refund);
        log.warn("订单关闭后收到支付成功回调，已创建自动退款单, orderNo={}, transactionId={}, outRefundNo={}",
                order.getOrderNo(), transactionId, refund.getOutRefundNo());
    }
}
