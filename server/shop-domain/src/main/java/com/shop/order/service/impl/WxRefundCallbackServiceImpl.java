package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantWechatConfig;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.service.MerchantWechatConfigService;
import com.shop.merchant.service.PaymentCredentialCipher;
import com.shop.order.entity.Order;
import com.shop.order.entity.RefundApplication;
import com.shop.order.enums.RefundStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.mapper.RefundApplicationMapper;
import com.shop.order.service.WxRefundCallbackService;
import com.shop.order.service.RefundCompletionService;
import com.shop.order.service.WxPayCallbackService.WxPayCallbackHeaders;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxRefundCallbackServiceImpl implements WxRefundCallbackService {

    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final PaymentCredentialCipher paymentCredentialCipher;
    private final MerchantWechatConfigService merchantWechatConfigService;
    private final RefundCompletionService refundCompletionService;

    @Override
    @Transactional
    public void handle(String merchantCode, WxPayCallbackHeaders headers, String rawBody) {
        Merchant merchant = getPayReadyMerchant(merchantCode);
        RefundNotification notification;
        try {
            MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(headers.serial())
                    .timestamp(headers.timestamp())
                    .nonce(headers.nonce())
                    .signature(headers.signature())
                    .body(rawBody)
                    .build();
            notification = new NotificationParser(buildNotificationConfig(config))
                    .parse(requestParam, RefundNotification.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信退款回调验签或解密失败, merchantCode={}", merchantCode, e);
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED);
        }

        if (notification == null || !hasText(notification.getOutRefundNo())
                || notification.getRefundStatus() == null) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED);
        }

        RefundApplication app = refundApplicationMapper.selectOne(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOutRefundNo, notification.getOutRefundNo())
                .last("FOR UPDATE"));
        if (app == null || !merchant.getId().equals(app.getMerchantId())) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "退款单不存在或商户不匹配");
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, app.getOrderNo()));
        if (order == null || !merchant.getId().equals(order.getMerchantId())) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "退款订单不存在或商户不匹配");
        }
        if (!app.getOrderNo().equals(notification.getOutTradeNo())
                || (hasText(order.getPayTransactionId()) && hasText(notification.getTransactionId())
                && !order.getPayTransactionId().equals(notification.getTransactionId()))) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "退款订单交易信息不匹配");
        }
        BigDecimal expectedRefundAmount = app.getRefundAmount() == null
                ? order.getPayAmount() : app.getRefundAmount();
        if (notification.getAmount() == null || notification.getAmount().getRefund() == null
                || notification.getAmount().getRefund() != yuanToFen(expectedRefundAmount)) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_AMOUNT_MISMATCH);
        }

        // 微信会重复通知；成功状态是终态，重复成功通知直接幂等返回。
        if (app.getStatus() == RefundStatus.SUCCESS.getCode()
                && notification.getRefundStatus() == Status.SUCCESS) {
            refundCompletionService.completeIfFullRefund(app, order,
                    app.getRefundTime() == null ? LocalDateTime.now() : app.getRefundTime());
            return;
        }

        app.setWxRefundId(notification.getRefundId() == null ? "" : notification.getRefundId());
        app.setRefundRawPayload(rawBody);
        if (notification.getRefundStatus() == Status.SUCCESS) {
            app.setStatus(RefundStatus.SUCCESS.getCode());
            app.setRefundTime(parseTime(notification.getSuccessTime()));
        } else if (notification.getRefundStatus() == Status.PROCESSING) {
            app.setStatus(RefundStatus.REFUNDING.getCode());
        } else {
            app.setStatus(RefundStatus.FAILED.getCode());
            app.setRefundFailReason("微信退款状态：" + notification.getRefundStatus().name());
        }
        app.setUpdatedAt(LocalDateTime.now());
        refundApplicationMapper.updateById(app);
        if (app.getStatus() == RefundStatus.SUCCESS.getCode()) {
            refundCompletionService.completeIfFullRefund(app, order, app.getRefundTime());
        }
        log.info("微信退款回调处理成功, merchantCode={}, orderNo={}, outRefundNo={}, status={}",
                merchantCode, app.getOrderNo(), app.getOutRefundNo(), notification.getRefundStatus());
    }

    private Merchant getPayReadyMerchant(String merchantCode) {
        if (!hasText(merchantCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getMerchantCode, merchantCode));
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        if (!Integer.valueOf(1).equals(config.getWxPayEnabled()) || !hasText(config.getWxMchId())
                || !hasText(config.getWxPayApiV3Key()) || !hasText(config.getWxPayMchSerialNo())
                || !hasText(config.getWxPayPrivateKey())) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE);
        }
        return merchant;
    }

    private NotificationConfig buildNotificationConfig(MerchantWechatConfig config) {
        String merchantId = config.getWxMchId().trim();
        String privateKey = normalizePrivateKey(paymentCredentialCipher.decrypt(config.getWxPayPrivateKey()));
        String serial = config.getWxPayMchSerialNo().trim();
        String apiV3Key = paymentCredentialCipher.decrypt(config.getWxPayApiV3Key()).trim();
        if (hasText(config.getWxPayPublicKey()) && hasText(config.getWxPayPublicKeyId())) {
            return new RSAPublicKeyConfig.Builder()
                    .merchantId(merchantId).privateKey(privateKey).merchantSerialNumber(serial)
                    .publicKey(paymentCredentialCipher.decrypt(config.getWxPayPublicKey()))
                    .publicKeyId(config.getWxPayPublicKeyId().trim()).apiV3Key(apiV3Key).build();
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId).privateKey(privateKey).merchantSerialNumber(serial)
                .apiV3Key(apiV3Key).build();
    }

    private LocalDateTime parseTime(String value) {
        if (!hasText(value)) return LocalDateTime.now();
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            log.warn("微信退款成功时间格式异常, value={}", value);
            return LocalDateTime.now();
        }
    }

    private int yuanToFen(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_AMOUNT_MISMATCH);
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
    }

    private String normalizePrivateKey(String value) {
        return value.trim().replace("\\n", "\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
