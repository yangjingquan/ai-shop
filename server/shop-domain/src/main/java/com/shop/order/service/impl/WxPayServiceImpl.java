package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.dto.OrderCreateVO;
import com.shop.order.entity.Order;
import com.shop.order.service.WxPayService;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayServiceImpl implements WxPayService {

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;

    @Override
    public OrderCreateVO.PayParams createJsapiPayParams(Order order) {
        Merchant merchant = getPayReadyMerchant(order.getMerchantId());
        User user = userMapper.selectById(order.getUserId());
        if (user == null || !hasText(user.getOpenid())) {
            throw new BusinessException(ErrorCode.WX_PAY_PREPAY_FAILED.getCode(), "用户 openid 缺失，请重新登录");
        }

        try {
            JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                    .config(buildConfig(merchant))
                    .build();

            PrepayRequest request = new PrepayRequest();
            request.setAppid(merchant.getWxAppId());
            request.setMchid(merchant.getWxMchId());
            request.setDescription("商城订单 " + order.getOrderNo());
            request.setOutTradeNo(order.getOrderNo());
            request.setNotifyUrl(merchant.getWxPayNotifyUrl());

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

    private Merchant getPayReadyMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getId, merchantId));
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MERCHANT_FROZEN);
        }
        if (merchant.getWxPayEnabled() == null || merchant.getWxPayEnabled() != 1
                || !hasText(merchant.getWxAppId())
                || !hasText(merchant.getWxMchId())
                || !hasText(merchant.getWxPayApiV3Key())
                || !hasText(merchant.getWxPayMchSerialNo())
                || !hasText(merchant.getWxPayPrivateKey())
                || !hasText(merchant.getWxPayNotifyUrl())) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE);
        }
        return merchant;
    }

    private RSAAutoCertificateConfig buildConfig(Merchant merchant) {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(merchant.getWxMchId().trim())
                .privateKey(normalizePrivateKey(merchant.getWxPayPrivateKey()))
                .merchantSerialNumber(merchant.getWxPayMchSerialNo().trim())
                .apiV3Key(merchant.getWxPayApiV3Key().trim())
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

    private String normalizePrivateKey(String privateKey) {
        return privateKey.trim().replace("\\n", "\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
