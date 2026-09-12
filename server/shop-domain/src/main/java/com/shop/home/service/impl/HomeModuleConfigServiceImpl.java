package com.shop.home.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.cache.PublicApiCacheService;
import com.shop.home.dto.HomeModuleUpdateRequest;
import com.shop.home.dto.HomeModuleVO;
import com.shop.home.entity.HomeModuleConfig;
import com.shop.home.mapper.HomeModuleConfigMapper;
import com.shop.home.service.HomeModuleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeModuleConfigServiceImpl implements HomeModuleConfigService {
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("BANNER", "焦点 Banner", "首页主视觉与运营活动", 10, null, null),
            new Definition("CATEGORY", "分类入口", "快速进入店铺分类", 20, null, null),
            new Definition("NEW_ARRIVALS", "新品速览", "按商品添加时间展示", 30, "RECENT", 2),
            new Definition("POPULAR_PRODUCTS", "人气推荐", "横向展示热销或推荐商品", 40, "TOP_SALES", 8),
            new Definition("PRODUCT_FEED", "商品精选流", "双列连续浏览商品", 50, "RECOMMEND", 10),
            new Definition("MARKETING_ZONE", "营销会场", "预售、积分、秒杀、团购等活动入口", 60, null, null)
    );
    private static final Map<String, Definition> DEFINITION_MAP = DEFINITIONS.stream()
            .collect(Collectors.toMap(Definition::code, item -> item, (a, b) -> a, LinkedHashMap::new));
    private static final List<String> PRODUCT_SOURCES = List.of("RECENT", "TOP_SALES", "RECOMMEND");

    private final HomeModuleConfigMapper mapper;

    @Autowired(required = false)
    private PublicApiCacheService publicApiCacheService;

    @Override
    public List<HomeModuleVO> list(Long merchantId) {
        Map<String, HomeModuleConfig> saved = mapper.selectList(new LambdaQueryWrapper<HomeModuleConfig>()
                        .eq(HomeModuleConfig::getMerchantId, merchantId))
                .stream().collect(Collectors.toMap(HomeModuleConfig::getModuleCode, item -> item, (a, b) -> a));
        return DEFINITIONS.stream().map(definition -> toVO(definition, saved.get(definition.code())))
                .sorted(Comparator.comparing(HomeModuleVO::getSortOrder).thenComparing(HomeModuleVO::getCode))
                .toList();
    }

    @Override
    public List<HomeModuleVO> listEnabled(Long merchantId) {
        return list(merchantId).stream().filter(item -> Integer.valueOf(1).equals(item.getEnabled())).toList();
    }

    @Override
    @Transactional
    public void update(Long merchantId, HomeModuleUpdateRequest request) {
        Map<String, HomeModuleUpdateRequest.Item> incoming = request.getModules().stream()
                .collect(Collectors.toMap(HomeModuleUpdateRequest.Item::getCode, item -> item, (a, b) -> {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "首页模块不能重复");
                }, LinkedHashMap::new));
        if (!DEFINITION_MAP.keySet().equals(incoming.keySet())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "首页模块配置不完整或包含未知模块");
        }
        Map<String, HomeModuleConfig> saved = mapper.selectList(new LambdaQueryWrapper<HomeModuleConfig>()
                        .eq(HomeModuleConfig::getMerchantId, merchantId))
                .stream().collect(Collectors.toMap(HomeModuleConfig::getModuleCode, item -> item, (a, b) -> a));
        for (Definition definition : DEFINITIONS) {
            HomeModuleUpdateRequest.Item item = incoming.get(definition.code());
            validate(definition, item);
            HomeModuleConfig entity = saved.get(definition.code());
            if (entity == null) {
                entity = new HomeModuleConfig();
                entity.setMerchantId(merchantId);
                entity.setModuleCode(definition.code());
                apply(entity, definition, item);
                mapper.insert(entity);
            } else {
                apply(entity, definition, item);
                mapper.updateById(entity);
            }
        }
        if (publicApiCacheService != null) {
            publicApiCacheService.evictHome(merchantId);
        }
    }

    private void validate(Definition definition, HomeModuleUpdateRequest.Item item) {
        if (!definition.productModule() && (item.getProductSource() != null || item.getProductLimit() != null)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "非商品模块不能配置商品来源");
        }
        if (definition.productModule() && item.getProductSource() != null && !PRODUCT_SOURCES.contains(item.getProductSource())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商品来源无效");
        }
    }

    private void apply(HomeModuleConfig target, Definition definition, HomeModuleUpdateRequest.Item item) {
        target.setTitle(blankToNull(item.getTitle()));
        target.setSubtitle(blankToNull(item.getSubtitle()));
        target.setEnabled(item.getEnabled());
        target.setSortOrder(item.getSortOrder());
        target.setProductSource(definition.productModule() ? defaultIfBlank(item.getProductSource(), definition.productSource()) : null);
        target.setProductLimit(definition.productModule() ? (item.getProductLimit() == null ? definition.productLimit() : item.getProductLimit()) : null);
    }

    private HomeModuleVO toVO(Definition definition, HomeModuleConfig saved) {
        HomeModuleVO vo = new HomeModuleVO();
        vo.setCode(definition.code());
        vo.setName(definition.name());
        vo.setDescription(definition.description());
        vo.setTitle(saved == null ? definition.name() : defaultIfBlank(saved.getTitle(), definition.name()));
        vo.setSubtitle(saved == null ? null : blankToNull(saved.getSubtitle()));
        vo.setEnabled(saved == null ? 1 : saved.getEnabled());
        vo.setSortOrder(saved == null ? definition.sortOrder() : saved.getSortOrder());
        vo.setProductSource(definition.productModule() ? (saved == null ? definition.productSource() : defaultIfBlank(saved.getProductSource(), definition.productSource())) : null);
        vo.setProductLimit(definition.productModule() ? (saved == null || saved.getProductLimit() == null ? definition.productLimit() : saved.getProductLimit()) : null);
        return vo;
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String defaultIfBlank(String value, String fallback) { String normalized = blankToNull(value); return normalized == null ? fallback : normalized; }

    private record Definition(String code, String name, String description, int sortOrder, String productSource, Integer productLimit) {
        boolean productModule() { return productSource != null; }
    }
}
