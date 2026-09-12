package com.shop.home.service.impl;

import com.shop.banner.service.BannerService;
import com.shop.common.cache.PublicApiCacheService;
import com.shop.home.dto.HomeVO;
import com.shop.home.service.HomeService;
import com.shop.home.service.HomeModuleConfigService;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.marketing.enums.MarketingActivityCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HomeServiceImpl implements HomeService {

    private final BannerService bannerService;
    private final ProductService productService;
    private final MarketingFeatureService marketingFeatureService;
    private final HomeModuleConfigService homeModuleConfigService;

    @Autowired(required = false)
    private PublicApiCacheService publicApiCacheService;

    @Autowired
    public HomeServiceImpl(BannerService bannerService, ProductService productService,
                           MarketingFeatureService marketingFeatureService,
                           HomeModuleConfigService homeModuleConfigService) {
        this.bannerService = bannerService;
        this.productService = productService;
        this.marketingFeatureService = marketingFeatureService;
        this.homeModuleConfigService = homeModuleConfigService;
    }

    /** 保留旧的单测/嵌入式调用构造方式，未注入营销能力时不返回任何活动入口。 */
    public HomeServiceImpl(BannerService bannerService, ProductService productService) {
        this(bannerService, productService, null, null);
    }

    @Override
    public HomeVO getHome(Long merchantId) {
        if (publicApiCacheService != null) {
            return publicApiCacheService.home(merchantId, () -> loadHome(merchantId));
        }
        return loadHome(merchantId);
    }

    private HomeVO loadHome(Long merchantId) {
        HomeVO vo = new HomeVO();
        vo.setBanners(bannerService.listActive(merchantId));
        vo.setMarketingFeatures(marketingFeatureService == null ? List.of() : marketingFeatureService.listEnabled(merchantId));

        // 最近上架（按 id 倒序即按时间倒序）
        var recent = productService.publicPage(1, 10, merchantId, null, null, null, null).getList();
        // 销量 Top 必须由数据库按 total_sales 排序，不能复用最新商品排序。
        var topSales = productService.publicTopSalesPage(1, 10, merchantId, null);
        List<ProductListVO> all = Stream.concat(recent.stream(), topSales.getList().stream())
                .collect(Collectors.toList());

        if (marketingFeatureService == null
                || !marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.GROUP_BUY)) {
            all.forEach(product -> {
                product.setIsGroupBuy(0);
                product.setGroupBuyPrice(null);
                product.setGroupBuyRequiredCount(null);
            });
        }

        // 去重（by productId）
        Set<Long> seen = new LinkedHashSet<>();
        List<ProductListVO> recommends = all.stream()
                .filter(p -> seen.add(p.getId()))
                .limit(12)
                .collect(Collectors.toList());

        vo.setRecommends(recommends);
        if (homeModuleConfigService != null) {
            var modules = homeModuleConfigService.listEnabled(merchantId);
            modules.forEach(module -> {
                if (module.getProductSource() != null) {
                    module.setProducts(loadModuleProducts(merchantId, module.getProductSource(), module.getProductLimit()));
                }
            });
            vo.setModules(modules);
        } else {
            vo.setModules(List.of());
        }
        return vo;
    }

    private List<ProductListVO> loadModuleProducts(Long merchantId, String source, Integer limit) {
        int size = limit == null ? 10 : limit;
        List<ProductListVO> products = switch (source) {
            case "RECENT" -> productService.publicPage(1, size, merchantId, null, null, null, null).getList();
            case "TOP_SALES" -> productService.publicTopSalesPage(1, size, merchantId, null).getList();
            case "RECOMMEND" -> productService.publicPage(1, size, merchantId, null, null, 1, null).getList();
            default -> List.of();
        };
        if (marketingFeatureService == null
                || !marketingFeatureService.isEnabled(merchantId, MarketingActivityCode.GROUP_BUY)) {
            products.forEach(product -> {
                product.setIsGroupBuy(0);
                product.setGroupBuyPrice(null);
                product.setGroupBuyRequiredCount(null);
            });
        }
        return products;
    }
}
