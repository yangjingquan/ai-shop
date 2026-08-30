package com.shop.home.service.impl;

import com.shop.banner.service.BannerService;
import com.shop.home.dto.HomeVO;
import com.shop.home.service.HomeService;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final BannerService bannerService;
    private final ProductService productService;

    @Override
    public HomeVO getHome(Long merchantId) {
        HomeVO vo = new HomeVO();
        vo.setBanners(bannerService.listActive(merchantId));

        // 最近上架（按 id 倒序即按时间倒序）
        var recent = productService.publicPage(1, 10, merchantId, null, null, null, null).getList();
        // 销量 Top 必须由数据库按 total_sales 排序，不能复用最新商品排序。
        var topSales = productService.publicTopSalesPage(1, 10, merchantId, null);
        List<ProductListVO> all = Stream.concat(recent.stream(), topSales.getList().stream())
                .collect(Collectors.toList());

        // 去重（by productId）
        Set<Long> seen = new LinkedHashSet<>();
        List<ProductListVO> recommends = all.stream()
                .filter(p -> seen.add(p.getId()))
                .limit(12)
                .collect(Collectors.toList());

        vo.setRecommends(recommends);
        return vo;
    }
}
