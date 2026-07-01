package com.shop.banner.service;

import com.shop.banner.dto.BannerSaveRequest;
import com.shop.banner.dto.BannerVO;
import com.shop.common.response.PageResult;

import java.util.List;

public interface BannerService {

    Long create(BannerSaveRequest req);

    void update(Long id, BannerSaveRequest req);

    void delete(Long id);

    PageResult<BannerVO> page(int page, int size);

    /** 给 /api/public/banner/list 返回 status=1 的活跃轮播 */
    List<BannerVO> listActive();
}
