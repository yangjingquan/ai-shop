package com.shop.banner.service;

import com.shop.banner.dto.BannerSaveRequest;
import com.shop.banner.dto.BannerVO;
import com.shop.common.response.PageResult;

import java.util.List;

public interface BannerService {

    default Long create(BannerSaveRequest req) {
        return create(null, req);
    }

    default void update(Long id, BannerSaveRequest req) {
        update(null, id, req);
    }

    default void delete(Long id) {
        delete(null, id);
    }

    default PageResult<BannerVO> page(int page, int size) {
        return page(null, page, size);
    }

    /** 给 /api/public/banner/list 返回当前商家的 status=1 活跃轮播。 */
    default List<BannerVO> listActive() {
        return listActive(null);
    }

    Long create(Long merchantId, BannerSaveRequest req);

    void update(Long merchantId, Long id, BannerSaveRequest req);

    void delete(Long merchantId, Long id);

    PageResult<BannerVO> page(Long merchantId, int page, int size);

    List<BannerVO> listActive(Long merchantId);
}
