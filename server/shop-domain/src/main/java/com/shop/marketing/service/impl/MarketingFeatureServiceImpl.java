package com.shop.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.marketing.dto.MarketingFeatureVO;
import com.shop.marketing.entity.MerchantMarketingFeature;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.mapper.MerchantMarketingFeatureMapper;
import com.shop.marketing.service.MarketingFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketingFeatureServiceImpl implements MarketingFeatureService {
    private static final String CACHE_PREFIX = "merchant:marketing:features:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String GROUP_BUY_CONFIG = "{\"durationHours\":24,\"userLimit\":1,\"showActiveGroups\":1,\"formedTemplateId\":\"sg0sw0AxgcxKZN1_Rz03ggc50HltbY1FK-Me2ZDGWcc\",\"expiringTemplateId\":\"RevYrSvVjLuJ4WEhySpfQ2FrWEyDKGyxcHYz-QiyzN0\",\"failedTemplateId\":\"9eLlvp1elpSJeHU-BgET6tZL2NOaqZfj6CB8vTX8s0A\"}";

    private final MerchantMarketingFeatureMapper featureMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<MarketingFeatureVO> list(Long merchantId) {
        Map<String, MerchantMarketingFeature> stored = featureMap(merchantId);
        return Arrays.stream(MarketingActivityCode.values())
                .map(code -> toVO(code, stored.get(code.getCode())))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketingFeatureVO> listEnabled(Long merchantId) {
        Set<String> enabledCodes = enabledCodes(merchantId);
        return Arrays.stream(MarketingActivityCode.values())
                .filter(MarketingActivityCode::isImplemented)
                .filter(code -> enabledCodes.contains(code.getCode()))
                .map(code -> {
                    MarketingFeatureVO vo = toVO(code, null);
                    vo.setEnabled(1);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabled(Long merchantId, MarketingActivityCode code) {
        return merchantId != null && code != null && code.isImplemented() && enabledCodes(merchantId).contains(code.getCode());
    }

    @Override
    public void assertEnabled(Long merchantId, MarketingActivityCode code) {
        if (!isEnabled(merchantId, code)) {
            throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_DISABLED);
        }
    }

    @Override
    @Transactional
    public void initializeMerchant(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        Map<String, MerchantMarketingFeature> stored = featureMap(merchantId);
        for (MarketingActivityCode code : MarketingActivityCode.values()) {
            if (stored.containsKey(code.getCode())) {
                continue;
            }
            MerchantMarketingFeature feature = new MerchantMarketingFeature();
            feature.setMerchantId(merchantId);
            feature.setFeatureCode(code.getCode());
            feature.setEnabled(0);
            feature.setConfigJson(defaultConfig(code));
            feature.setSort(code.ordinal() + 1);
            feature.setVersion(0);
            featureMapper.insert(feature);
        }
        evict(merchantId);
    }

    @Override
    @Transactional
    public void update(Long merchantId, String code, Integer enabled, Long operatorId) {
        MarketingActivityCode activity = MarketingActivityCode.fromCode(code);
        if (activity == null) {
            throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND);
        }
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        MerchantMarketingFeature feature = featureMapper.selectOne(new LambdaQueryWrapper<MerchantMarketingFeature>()
                .eq(MerchantMarketingFeature::getMerchantId, merchantId)
                .eq(MerchantMarketingFeature::getFeatureCode, activity.getCode()));
        if (feature == null) {
            feature = new MerchantMarketingFeature();
            feature.setMerchantId(merchantId);
            feature.setFeatureCode(activity.getCode());
            feature.setConfigJson(defaultConfig(activity));
            feature.setSort(activity.ordinal() + 1);
            feature.setVersion(0);
        }
        feature.setEnabled(enabled);
        feature.setUpdatedBy(operatorId);
        feature.setVersion(feature.getVersion() == null ? 1 : feature.getVersion() + 1);
        if (feature.getId() == null) {
            featureMapper.insert(feature);
        } else {
            featureMapper.updateById(feature);
        }
        evict(merchantId);
    }

    private Map<String, MerchantMarketingFeature> featureMap(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyMap();
        }
        return featureMapper.selectList(new LambdaQueryWrapper<MerchantMarketingFeature>()
                        .eq(MerchantMarketingFeature::getMerchantId, merchantId)).stream()
                .collect(Collectors.toMap(MerchantMarketingFeature::getFeatureCode, item -> item, (left, right) -> left));
    }

    private Set<String> enabledCodes(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptySet();
        }
        String key = CACHE_PREFIX + merchantId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached.isBlank() ? Collections.emptySet() : Arrays.stream(cached.split(",")).collect(Collectors.toSet());
            }
        } catch (RuntimeException ex) {
            log.warn("读取商家营销能力缓存失败，降级查询数据库 merchantId={}", merchantId, ex);
        }

        Set<String> result;
        try {
            result = featureMap(merchantId).values().stream()
                    .filter(item -> Integer.valueOf(1).equals(item.getEnabled()))
                    .map(MerchantMarketingFeature::getFeatureCode)
                    .collect(Collectors.toSet());
        } catch (RuntimeException ex) {
            // 能力查询失败时宁可关闭入口，避免缓存/数据库故障导致活动越权可用。
            log.error("查询商家营销能力失败，按关闭处理 merchantId={}", merchantId, ex);
            return Collections.emptySet();
        }
        try {
            stringRedisTemplate.opsForValue().set(key, String.join(",", result), CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("写入商家营销能力缓存失败 merchantId={}", merchantId, ex);
        }
        return result;
    }

    private void evict(Long merchantId) {
        try {
            stringRedisTemplate.delete(CACHE_PREFIX + merchantId);
        } catch (RuntimeException ex) {
            log.warn("清理商家营销能力缓存失败 merchantId={}", merchantId, ex);
        }
    }

    private MarketingFeatureVO toVO(MarketingActivityCode code, MerchantMarketingFeature stored) {
        MarketingFeatureVO vo = new MarketingFeatureVO();
        vo.setCode(code.getCode());
        vo.setName(code.getName());
        vo.setDescription(code.getDescription());
        vo.setEnabled(stored == null || stored.getEnabled() == null ? 0 : stored.getEnabled());
        vo.setImplemented(code.isImplemented());
        vo.setFrontendPath(code.getFrontendPath());
        vo.setSort(stored == null || stored.getSort() == null ? code.ordinal() + 1 : stored.getSort());
        return vo;
    }

    private String defaultConfig(MarketingActivityCode code) {
        return code == MarketingActivityCode.GROUP_BUY ? GROUP_BUY_CONFIG : "{}";
    }
}
