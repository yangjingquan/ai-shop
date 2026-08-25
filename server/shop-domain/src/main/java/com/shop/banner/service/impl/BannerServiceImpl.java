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
import com.shop.product.entity.MerchantCategory;
import com.shop.product.entity.Product;
import com.shop.product.mapper.MerchantCategoryMapper;
import com.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;
    private final ProductMapper productMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;

    @Override
    @Transactional
    public Long create(Long merchantId, BannerSaveRequest req) {
        validateRequest(merchantId, req);
        Banner b = new Banner();
        b.setMerchantId(merchantId);
        b.setImageUrl(req.getImageUrl());
        b.setLinkType(normalizeLinkType(req.getLinkType()));
        b.setLinkValue(normalizeLinkValue(req));
        b.setSort(req.getSort() != null ? req.getSort() : 0);
        b.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        bannerMapper.insert(b);
        return b.getId();
    }

    @Override
    @Transactional
    public void update(Long merchantId, Long id, BannerSaveRequest req) {
        validateRequest(merchantId, req);
        Banner b = findOwned(id, merchantId);
        if (b == null) throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        b.setImageUrl(req.getImageUrl());
        b.setLinkType(normalizeLinkType(req.getLinkType()));
        b.setLinkValue(normalizeLinkValue(req));
        b.setSort(req.getSort() != null ? req.getSort() : 0);
        b.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        bannerMapper.updateById(b);
    }

    @Override
    @Transactional
    public void delete(Long merchantId, Long id) {
        Banner b = findOwned(id, merchantId);
        if (b == null) throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        bannerMapper.deleteById(id);
    }

    @Override
    public PageResult<BannerVO> page(Long merchantId, int page, int size) {
        LambdaQueryWrapper<Banner> query = scopeQuery(merchantId);
        IPage<Banner> result = bannerMapper.selectPage(
                new Page<>(page, size),
                query.orderByAsc(Banner::getSort).orderByDesc(Banner::getId));
        List<BannerVO> list = new ArrayList<>();
        for (Banner b : result.getRecords()) {
            list.add(toVO(b));
        }
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public List<BannerVO> listActive(Long merchantId) {
        LambdaQueryWrapper<Banner> query = new LambdaQueryWrapper<>();
        if (merchantId == null) {
            query.isNull(Banner::getMerchantId);
        } else {
            query.and(w -> w.eq(Banner::getMerchantId, merchantId)
                    .or().isNull(Banner::getMerchantId));
        }
        List<Banner> banners = bannerMapper.selectList(
                query
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

    private LambdaQueryWrapper<Banner> scopeQuery(Long merchantId) {
        LambdaQueryWrapper<Banner> query = new LambdaQueryWrapper<>();
        if (merchantId == null) {
            query.isNull(Banner::getMerchantId);
        } else {
            query.eq(Banner::getMerchantId, merchantId);
        }
        return query;
    }

    private Banner findOwned(Long id, Long merchantId) {
        return bannerMapper.selectOne(scopeQuery(merchantId).eq(Banner::getId, id));
    }

    private void validateRequest(Long merchantId, BannerSaveRequest req) {
        if (req == null || req.getLinkType() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "跳转类型不能为空");
        }
        int linkType = req.getLinkType();
        String linkValue = req.getLinkValue() == null ? "" : req.getLinkValue().trim();
        if (linkType < 0 || linkType > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "跳转类型不合法");
        }
        if (linkType == 0) {
            return;
        }
        if (!StringUtils.hasText(linkValue)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "跳转目标不能为空");
        }
        if (linkType == 3) {
            validateExternalUrl(linkValue);
            return;
        }

        // 兼容历史上把 linkType=1 当作任意小程序页面路径保存的 Banner。
        if (linkType == 1 && linkValue.startsWith("/pages/") && !linkValue.contains("//")) {
            return;
        }

        Long targetId = parseTargetId(linkValue);
        if (merchantId == null) {
            return;
        }
        if (linkType == 1) {
            Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                    .eq(Product::getId, targetId)
                    .eq(Product::getMerchantId, merchantId)
                    .last("limit 1"));
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (Integer.valueOf(1).equals(req.getStatus()) && !Integer.valueOf(1).equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
            }
        } else {
            MerchantCategory category = merchantCategoryMapper.selectOne(new LambdaQueryWrapper<MerchantCategory>()
                    .eq(MerchantCategory::getId, targetId)
                    .eq(MerchantCategory::getMerchantId, merchantId)
                    .last("limit 1"));
            if (category == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            if (Integer.valueOf(1).equals(req.getStatus()) && !Integer.valueOf(1).equals(category.getStatus())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }
    }

    private int normalizeLinkType(Integer linkType) {
        return linkType == null ? 0 : linkType;
    }

    private String normalizeLinkValue(BannerSaveRequest req) {
        if (req.getLinkType() == null || req.getLinkType() == 0 || req.getLinkValue() == null) {
            return "";
        }
        return req.getLinkValue().trim();
    }

    private Long parseTargetId(String value) {
        if (!value.matches("[1-9][0-9]*")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商品或分类 ID 必须为正整数");
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商品或分类 ID 不合法");
        }
    }

    private void validateExternalUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "外部链接必须是合法的 HTTPS 地址");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "外部链接格式不合法");
        }
    }
}
