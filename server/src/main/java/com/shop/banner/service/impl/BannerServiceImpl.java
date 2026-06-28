package com.shop.banner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.banner.dto.BannerSaveRequest;
import com.shop.banner.dto.BannerVO;
import com.shop.banner.entity.Banner;
import com.shop.banner.mapper.BannerMapper;
import com.shop.banner.service.BannerService;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;

    @Override
    @Transactional
    public Long create(BannerSaveRequest req) {
        Banner b = new Banner();
        b.setImageUrl(req.getImageUrl());
        b.setLinkType(req.getLinkType());
        b.setLinkValue(req.getLinkValue() != null ? req.getLinkValue() : "");
        b.setSort(req.getSort() != null ? req.getSort() : 0);
        b.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        bannerMapper.insert(b);
        return b.getId();
    }

    @Override
    @Transactional
    public void update(Long id, BannerSaveRequest req) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        b.setImageUrl(req.getImageUrl());
        b.setLinkType(req.getLinkType());
        b.setLinkValue(req.getLinkValue() != null ? req.getLinkValue() : "");
        b.setSort(req.getSort() != null ? req.getSort() : 0);
        b.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        bannerMapper.updateById(b);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        bannerMapper.deleteById(id);
    }

    @Override
    public PageResult<BannerVO> page(int page, int size) {
        IPage<Banner> result = bannerMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSort).orderByDesc(Banner::getId));
        List<BannerVO> list = new ArrayList<>();
        for (Banner b : result.getRecords()) {
            list.add(toVO(b));
        }
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public List<BannerVO> listActive() {
        List<Banner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, 1)
                        .orderByAsc(Banner::getSort));
        List<BannerVO> list = new ArrayList<>();
        for (Banner b : banners) {
            list.add(toVO(b));
        }
        return list;
    }

    private BannerVO toVO(Banner b) {
        BannerVO vo = new BannerVO();
        vo.setId(b.getId());
        vo.setImageUrl(b.getImageUrl());
        vo.setLinkType(b.getLinkType());
        vo.setLinkValue(b.getLinkValue());
        vo.setSort(b.getSort());
        vo.setStatus(b.getStatus());
        vo.setCreatedAt(b.getCreatedAt());
        return vo;
    }
}
