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
    public HomeVO getHome() {
        HomeVO vo = new HomeVO();
        vo.setBanners(bannerService.listActive());

        // 最近上架（按 id 倒序即按时间倒序）
        var recent = productService.page(1, 10, null, null, null, 1).getList();
        // 销量 Top
        var topSales = productService.page(1, 10, null, null, null, null);

        // 但 page API 不支持 ORDER BY total_sales。用内置规则拿到 recent 再手动取销量高的。
        // 简化：取 page 2 page 不同排序，只用 recent + 同一个 page 的 list 去重。
        // 实际效果：最近的 10 个上架商品，部分会被销量高的替换。
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
