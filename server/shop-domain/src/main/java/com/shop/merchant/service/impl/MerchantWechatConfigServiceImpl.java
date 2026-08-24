package com.shop.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.merchant.dto.MerchantWechatSettingsVO;
import com.shop.merchant.dto.UpdateWechatSettingsRequest;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantWechatConfig;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.mapper.MerchantWechatConfigMapper;
import com.shop.merchant.service.MerchantWechatConfigService;
import com.shop.merchant.service.PaymentCredentialCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantWechatConfigServiceImpl implements MerchantWechatConfigService {

    private final MerchantWechatConfigMapper configMapper;
    private final MerchantMapper merchantMapper;
    private final PaymentCredentialCipher paymentCredentialCipher;

    @Override
    public MerchantWechatConfig getByMerchantId(Long merchantId) {
        return configMapper.selectOne(new LambdaQueryWrapper<MerchantWechatConfig>()
                .eq(MerchantWechatConfig::getMerchantId, merchantId)
                .last("LIMIT 1"));
    }

    @Override
    public MerchantWechatConfig getRequiredByMerchantId(Long merchantId) {
        MerchantWechatConfig config = getByMerchantId(merchantId);
        if (config == null) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE);
        }
        return config;
    }

    @Override
    public void ensureConfig(Long merchantId) {
        if (getByMerchantId(merchantId) != null) {
            return;
        }
        MerchantWechatConfig config = new MerchantWechatConfig();
        config.setMerchantId(merchantId);
        config.setWxAppId("");
        config.setWxSecret("");
        config.setWxMchId("");
        config.setWxPayApiV3Key("");
        config.setWxPayMchSerialNo("");
        config.setWxPayPrivateKey("");
        config.setWxPayNotifyUrl("");
        config.setWxPayEnabled(0);
        configMapper.insert(config);
    }

    @Override
    public PageResult<MerchantWechatSettingsVO> page(int page, int size, String keyword) {
        Page<Merchant> merchantPage = new Page<>(page, size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .orderByDesc(Merchant::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Merchant::getName, keyword)
                    .or().like(Merchant::getMerchantCode, keyword));
        }
        Page<Merchant> result = merchantMapper.selectPage(merchantPage, wrapper);
        List<Long> merchantIds = result.getRecords().stream().map(Merchant::getId).toList();
        Map<Long, MerchantWechatConfig> configMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            configMapper.selectList(new LambdaQueryWrapper<MerchantWechatConfig>()
                    .in(MerchantWechatConfig::getMerchantId, merchantIds))
                    .forEach(config -> configMap.put(config.getMerchantId(), config));
        }
        List<MerchantWechatSettingsVO> list = result.getRecords().stream()
                .map(merchant -> toVO(merchant, configMap.get(merchant.getId())))
                .toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public MerchantWechatSettingsVO getSettings(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        ensureConfig(merchantId);
        return toVO(merchant, getRequiredByMerchantId(merchantId));
    }

    @Override
    @Transactional
    public void updateSettings(Long merchantId, UpdateWechatSettingsRequest request) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        ensureConfig(merchantId);
        MerchantWechatConfig config = getRequiredByMerchantId(merchantId);

        if (request.getWxAppId() != null) config.setWxAppId(request.getWxAppId().trim());
        if (request.getWxSecret() != null && !request.getWxSecret().isBlank()) {
            config.setWxSecret(request.getWxSecret().trim());
        }
        if (request.getWxMchId() != null) config.setWxMchId(request.getWxMchId().trim());
        applyPaymentConfig(config, merchant.getMerchantCode(), request);
        if (request.getWxPayEnabled() != null) {
            config.setWxPayEnabled(request.getWxPayEnabled() == 1 ? 1 : 0);
        }
        validateEnabledPaymentConfig(config);
        configMapper.updateById(config);
    }

    private MerchantWechatSettingsVO toVO(Merchant merchant, MerchantWechatConfig config) {
        MerchantWechatSettingsVO vo = new MerchantWechatSettingsVO();
        vo.setMerchantId(merchant.getId());
        vo.setMerchantCode(merchant.getMerchantCode());
        vo.setMerchantName(merchant.getName());
        if (config == null) {
            vo.setWxAppId("");
            vo.setWxMchId("");
            vo.setWxSecretConfigured(false);
            vo.setWxPayMchSerialNo("");
            vo.setWxPayNotifyUrl("");
            vo.setWxPayEnabled(0);
            vo.setWxPayApiV3KeyConfigured(false);
            vo.setWxPayPrivateKeyConfigured(false);
            vo.setWxPayConfigured(false);
            return vo;
        }
        vo.setWxAppId(config.getWxAppId());
        vo.setWxMchId(config.getWxMchId());
        vo.setWxSecretConfigured(hasText(config.getWxSecret()));
        vo.setWxPayMchSerialNo(config.getWxPayMchSerialNo());
        vo.setWxPayNotifyUrl(config.getWxPayNotifyUrl());
        vo.setWxPayEnabled(config.getWxPayEnabled());
        vo.setWxPayApiV3KeyConfigured(hasText(config.getWxPayApiV3Key()));
        vo.setWxPayPrivateKeyConfigured(hasText(config.getWxPayPrivateKey()));
        vo.setWxPayConfigured(isPaymentConfigured(config));
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }

    private void applyPaymentConfig(MerchantWechatConfig config, String merchantCode,
                                    UpdateWechatSettingsRequest request) {
        if (request.getWxPayApiV3Key() != null && !request.getWxPayApiV3Key().isBlank()) {
            String normalized = request.getWxPayApiV3Key().trim();
            if (!normalized.matches("[A-Za-z0-9]{32}")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "API v3 密钥必须为 32 位字母或数字");
            }
            config.setWxPayApiV3Key(paymentCredentialCipher.encrypt(normalized));
        }
        if (request.getWxPayMchSerialNo() != null) {
            String normalized = request.getWxPayMchSerialNo().trim();
            if (!normalized.isEmpty() && !normalized.matches("[A-Fa-f0-9]{16,128}")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户证书序列号格式不正确");
            }
            config.setWxPayMchSerialNo(normalized);
        }
        if (request.getWxPayPrivateKey() != null && !request.getWxPayPrivateKey().isBlank()) {
            String normalized = request.getWxPayPrivateKey().trim().replace("\\r\\n", "\\n").replace("\\n", "\n");
            if (!normalized.contains("-----BEGIN PRIVATE KEY-----") || !normalized.contains("-----END PRIVATE KEY-----")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户私钥必须为 apiclient_key.pem 的 PEM 内容");
            }
            config.setWxPayPrivateKey(paymentCredentialCipher.encrypt(normalized));
        }
        if (request.getWxPayNotifyUrl() != null) {
            String normalized = request.getWxPayNotifyUrl().trim();
            if (!normalized.isEmpty()) {
                try {
                    URI uri = URI.create(normalized);
                    String expectedPath = "/api/callback/wxpay/" + merchantCode;
                    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                            || uri.getQuery() != null || !expectedPath.equals(uri.getPath())) {
                        throw new IllegalArgumentException("invalid notify url");
                    }
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "支付回调地址必须为无参数的 HTTPS 地址，且路径为 /api/callback/wxpay/" + merchantCode);
                }
            }
            config.setWxPayNotifyUrl(normalized);
        }
    }

    private void validateEnabledPaymentConfig(MerchantWechatConfig config) {
        if (!Integer.valueOf(1).equals(config.getWxPayEnabled())) {
            return;
        }
        if (!hasText(config.getWxAppId()) || !hasText(config.getWxMchId())
                || !hasText(config.getWxPayApiV3Key()) || !hasText(config.getWxPayMchSerialNo())
                || !hasText(config.getWxPayPrivateKey()) || !hasText(config.getWxPayNotifyUrl())) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE);
        }
        paymentCredentialCipher.decrypt(config.getWxPayApiV3Key());
        paymentCredentialCipher.decrypt(config.getWxPayPrivateKey());
    }

    private boolean isPaymentConfigured(MerchantWechatConfig config) {
        return hasText(config.getWxAppId()) && hasText(config.getWxMchId())
                && hasText(config.getWxPayApiV3Key()) && hasText(config.getWxPayMchSerialNo())
                && hasText(config.getWxPayPrivateKey()) && hasText(config.getWxPayNotifyUrl())
                && Integer.valueOf(1).equals(config.getWxPayEnabled());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
