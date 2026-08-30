package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantWechatConfig;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.service.MerchantWechatConfigService;
import com.shop.merchant.service.PaymentCredentialCipher;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.shop.order.service.WxPayService;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayServiceImpl implements WxPayService {

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final PaymentCredentialCipher paymentCredentialCipher;
    private final MerchantWechatConfigService merchantWechatConfigService;

    @Override
    public OrderCreateVO.PayParams createJsapiPayParams(Order order) {
        Merchant merchant = getPayReadyMerchant(order.getMerchantId());
        MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        User user = userMapper.selectById(order.getUserId());
        if (user == null || !hasText(user.getOpenid())) {
            throw new BusinessException(ErrorCode.WX_PAY_PREPAY_FAILED.getCode(), "用户 openid 缺失，请重新登录");
        }

        try {
            JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                    .config(buildConfig(config))
                    .build();

            PrepayRequest request = new PrepayRequest();
            request.setAppid(config.getWxAppId());
            request.setMchid(config.getWxMchId());
            request.setDescription("商城订单 " + order.getOrderNo());
            request.setOutTradeNo(order.getOrderNo());
            request.setNotifyUrl(config.getWxPayNotifyUrl());
            request.setTimeExpire(formatWxPayDateTime(OffsetDateTime.now(ZoneOffset.ofHours(8)).plusMinutes(30)));

            Amount amount = new Amount();
            amount.setTotal(yuanToFen(order.getPayAmount()));
            amount.setCurrency("CNY");
            request.setAmount(amount);

            Payer payer = new Payer();
            payer.setOpenid(user.getOpenid());
            request.setPayer(payer);

            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            OrderCreateVO.PayParams params = new OrderCreateVO.PayParams();
            params.setAppId(response.getAppId());
            params.setTimeStamp(response.getTimeStamp());
            params.setNonceStr(response.getNonceStr());
            params.setPackageStr(response.getPackageVal());
            params.setSignType(response.getSignType());
            params.setPaySign(response.getPaySign());
            return params;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付预下单失败, orderNo={}, merchantCode={}", order.getOrderNo(), merchant.getMerchantCode(), e);
            throw new BusinessException(ErrorCode.WX_PAY_PREPAY_FAILED);
        }
    }

    @Override
    public Transaction queryOrder(Order order) {
        Merchant merchant = getPayReadyMerchant(order.getMerchantId());
        MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        try {
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(config.getWxMchId().trim());
            request.setOutTradeNo(order.getOrderNo());
            return new JsapiServiceExtension.Builder().config(buildConfig(config)).build()
                    .queryOrderByOutTradeNo(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付查单失败, orderNo={}, merchantCode={}", order.getOrderNo(), merchant.getMerchantCode(), e);
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "微信支付状态查询失败，请稍后重试");
        }
    }

    @Override
    public void closeOrder(Order order) {
        Merchant merchant = getPayReadyMerchant(order.getMerchantId());
        MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        try {
            CloseOrderRequest request = new CloseOrderRequest();
            request.setMchid(config.getWxMchId().trim());
            request.setOutTradeNo(order.getOrderNo());
            new JsapiServiceExtension.Builder().config(buildConfig(config)).build().closeOrder(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付关单失败, orderNo={}, merchantCode={}", order.getOrderNo(), merchant.getMerchantCode(), e);
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "微信支付关单失败，请稍后重试");
        }
    }

    @Override
    public Refund createRefund(Order order, String outRefundNo, String reason, BigDecimal refundAmount) {
        Merchant merchant = getPayReadyMerchant(order.getMerchantId());
        MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        try {
            CreateRequest request = new CreateRequest();
            if (hasText(order.getPayTransactionId())) {
                request.setTransactionId(order.getPayTransactionId());
            } else {
                request.setOutTradeNo(order.getOrderNo());
            }
            request.setOutRefundNo(outRefundNo);
            request.setReason(hasText(reason) ? reason : "订单退款");
            // 支付回调地址已按商户代码校验，退款回调沿用同一域名和商户路径约定。
            request.setNotifyUrl(refundNotifyUrl(config.getWxPayNotifyUrl()));

            AmountReq amount = new AmountReq();
            long totalFen = yuanToFen(order.getPayAmount());
            long refundFen = yuanToFen(refundAmount);
            if (refundFen <= 0 || refundFen > totalFen) {
                throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "退款金额不合法");
            }
            amount.setTotal(totalFen);
            amount.setRefund(refundFen);
            amount.setCurrency("CNY");
            request.setAmount(amount);

            return new RefundService.Builder().config(buildConfig(config)).build().create(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信退款申请失败, orderNo={}, outRefundNo={}, merchantCode={}",
                    order.getOrderNo(), outRefundNo, merchant.getMerchantCode(), e);
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "微信退款申请失败，请稍后重试");
        }
    }

    @Override
    public Refund queryRefund(Order order, String outRefundNo) {
        Merchant merchant = getPayReadyMerchant(order.getMerchantId());
        MerchantWechatConfig config = merchantWechatConfigService.getRequiredByMerchantId(merchant.getId());
        try {
            QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
            request.setOutRefundNo(outRefundNo);
            return new RefundService.Builder().config(buildConfig(config)).build().queryByOutRefundNo(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信退款查询失败, orderNo={}, outRefundNo={}, merchantCode={}",
                    order.getOrderNo(), outRefundNo, merchant.getMerchantCode(), e);
            throw new BusinessException(ErrorCode.PAY_FAILED.getCode(), "微信退款状态查询失败，请稍后重试");
        }
    }

    private Merchant getPayReadyMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getId, merchantId));
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
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

    private Config buildConfig(MerchantWechatConfig config) {
        String merchantId = config.getWxMchId().trim();
        String privateKey = normalizePrivateKey(paymentCredentialCipher.decrypt(config.getWxPayPrivateKey()));
        String merchantSerialNumber = config.getWxPayMchSerialNo().trim();
        String apiV3Key = paymentCredentialCipher.decrypt(config.getWxPayApiV3Key()).trim();
        if (hasText(config.getWxPayPublicKey()) && hasText(config.getWxPayPublicKeyId())) {
            return new RSAPublicKeyConfig.Builder()
                    .merchantId(merchantId)
                    .privateKey(privateKey)
                    .merchantSerialNumber(merchantSerialNumber)
                    .publicKey(paymentCredentialCipher.decrypt(config.getWxPayPublicKey()))
                    .publicKeyId(config.getWxPayPublicKeyId().trim())
                    .apiV3Key(apiV3Key)
                    .build();
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId)
                .privateKey(privateKey)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }

    private int yuanToFen(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.WX_PAY_PREPAY_FAILED.getCode(), "订单支付金额为空");
        }
        return amount.movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .intValueExact();
    }

    static String formatWxPayDateTime(OffsetDateTime value) {
        return value.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    static String refundNotifyUrl(String payNotifyUrl) {
        if (!hasTextStatic(payNotifyUrl)) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE);
        }
        String marker = "/api/callback/wxpay/";
        int index = payNotifyUrl.indexOf(marker);
        if (index < 0) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE.getCode(), "支付回调地址格式不正确");
        }
        return payNotifyUrl.substring(0, index) + "/api/callback/wxrefund/"
                + payNotifyUrl.substring(index + marker.length());
    }

    private String normalizePrivateKey(String privateKey) {
        return privateKey.trim().replace("\\n", "\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasTextStatic(String value) {
        return value != null && !value.isBlank();
    }
}
