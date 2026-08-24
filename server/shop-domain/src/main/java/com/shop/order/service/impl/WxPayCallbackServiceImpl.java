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
import com.shop.order.service.OrderPaymentService;
import com.shop.order.service.WxPayCallbackService;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayCallbackServiceImpl implements WxPayCallbackService {

    private final MerchantMapper merchantMapper;
    private final com.shop.order.mapper.OrderMapper orderMapper;
    private final OrderPaymentService orderPaymentService;
    private final PaymentCredentialCipher paymentCredentialCipher;
    private final MerchantWechatConfigService merchantWechatConfigService;

    @Override
    public void handle(String merchantCode, WxPayCallbackHeaders headers, String rawBody) {
        Merchant merchant = getPayReadyMerchant(merchantCode);
        try {
            Transaction transaction = parseTransaction(merchant, headers, rawBody);
            validateTransaction(merchant, transaction, rawBody);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付回调验签或解密失败, merchantCode={}", merchantCode, e);
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED);
        }
    }

    private Transaction parseTransaction(Merchant merchant, WxPayCallbackHeaders headers, String rawBody) {
        MerchantWechatConfig wechatConfig = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        AutoCertificateNotificationConfig config = new AutoCertificateNotificationConfig.Builder()
                .merchantId(wechatConfig.getWxMchId().trim())
                .privateKey(normalizePrivateKey(paymentCredentialCipher.decrypt(wechatConfig.getWxPayPrivateKey())))
                .merchantSerialNumber(wechatConfig.getWxPayMchSerialNo().trim())
                .apiV3Key(paymentCredentialCipher.decrypt(wechatConfig.getWxPayApiV3Key()).trim())
                .build();

        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(headers.serial())
                .timestamp(headers.timestamp())
                .nonce(headers.nonce())
                .signature(headers.signature())
                .body(rawBody)
                .build();

        NotificationParser parser = new NotificationParser(config);
        return parser.parse(requestParam, Transaction.class);
    }

    private void validateTransaction(Merchant merchant, Transaction transaction, String rawBody) {
        if (transaction == null) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED);
        }
        if (transaction.getTradeState() != TradeStateEnum.SUCCESS) {
            log.warn("忽略非成功微信支付回调, merchantCode={}, orderNo={}, tradeState={}",
                    merchant.getMerchantCode(), transaction.getOutTradeNo(), transaction.getTradeState());
            return;
        }
        if (!hasText(transaction.getOutTradeNo()) || !hasText(transaction.getTransactionId())) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "微信支付回调订单号或交易号为空");
        }
        MerchantWechatConfig wechatConfig = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        if (!wechatConfig.getWxAppId().equals(transaction.getAppid())
                || !wechatConfig.getWxMchId().equals(transaction.getMchid())) {
            log.error("微信支付回调商户不匹配, merchantCode={}, orderNo={}, appid={}, mchid={}",
                    merchant.getMerchantCode(), transaction.getOutTradeNo(), transaction.getAppid(), transaction.getMchid());
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "微信支付回调商户不匹配");
        }

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, transaction.getOutTradeNo()));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!merchant.getId().equals(order.getMerchantId())) {
            log.error("微信支付回调订单商户不匹配, merchantCode={}, orderNo={}",
                    merchant.getMerchantCode(), order.getOrderNo());
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_VERIFY_FAILED.getCode(), "微信支付回调订单商户不匹配");
        }
        Integer callbackAmount = transaction.getAmount() == null ? null : transaction.getAmount().getTotal();
        if (callbackAmount == null || callbackAmount != yuanToFen(order.getPayAmount())) {
            log.error("微信支付回调金额不一致, merchantCode={}, orderNo={}, callbackAmount={}, orderAmount={}",
                    merchant.getMerchantCode(), order.getOrderNo(), callbackAmount, order.getPayAmount());
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_AMOUNT_MISMATCH);
        }

        orderPaymentService.handlePaidCallback(order.getOrderNo(), transaction.getTransactionId(), rawBody);
        log.info("微信支付回调处理成功, merchantCode={}, orderNo={}, transactionId={}",
                merchant.getMerchantCode(), order.getOrderNo(), transaction.getTransactionId());
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
        if (!Integer.valueOf(1).equals(config.getWxPayEnabled())
                || !hasText(config.getWxAppId())
                || !hasText(config.getWxMchId())
                || !hasText(config.getWxPayApiV3Key())
                || !hasText(config.getWxPayMchSerialNo())
                || !hasText(config.getWxPayPrivateKey())
                || !hasText(config.getWxPayNotifyUrl())) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE);
        }
        return merchant;
    }

    private int yuanToFen(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.WX_PAY_CALLBACK_AMOUNT_MISMATCH);
        }
        return amount.movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .intValueExact();
    }

    private String normalizePrivateKey(String privateKey) {
        return privateKey.trim().replace("\\n", "\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
