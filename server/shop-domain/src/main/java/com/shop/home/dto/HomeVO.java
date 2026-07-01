package com.shop.home.dto;

import com.shop.banner.dto.BannerVO;
import com.shop.product.dto.ProductListVO;
import lombok.Data;

import java.util.List;

@Data
public class HomeVO {

    private List<BannerVO> banners;

    private List<ProductListVO> recommends;
}
