package com.shop.home.dto;

import com.shop.banner.dto.BannerVO;
import com.shop.product.dto.ProductListVO;
import com.shop.marketing.dto.MarketingFeatureVO;
import lombok.Data;

import java.util.List;

@Data
public class HomeVO {

    private List<BannerVO> banners;

    private List<ProductListVO> recommends;

    private List<MarketingFeatureVO> marketingFeatures;
}
