package com.shop.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.product.dto.CategoryVO;
import com.shop.product.dto.MerchantCategoryImportRequest;
import com.shop.product.dto.MerchantCategoryRequest;
import com.shop.product.dto.MerchantCategoryVO;
import com.shop.product.entity.Category;
import com.shop.product.entity.MerchantCategory;
import com.shop.product.entity.Product;
import com.shop.product.mapper.CategoryMapper;
import com.shop.product.mapper.MerchantCategoryMapper;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.service.CategoryService;
import com.shop.product.service.MerchantCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MerchantCategoryServiceImpl implements MerchantCategoryService {

    private final MerchantCategoryMapper merchantCategoryMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;

    @Override
    public List<MerchantCategoryVO> tree(Long merchantId, boolean enabledOnly) {
        LambdaQueryWrapper<MerchantCategory> q = new LambdaQueryWrapper<MerchantCategory>()
                .eq(MerchantCategory::getMerchantId, merchantId)
                .orderByAsc(MerchantCategory::getSort)
                .orderByAsc(MerchantCategory::getId);
        if (enabledOnly) {
            q.eq(MerchantCategory::getStatus, 1);
        }
        return buildTree(merchantCategoryMapper.selectList(q));
    }

    @Override
    public List<CategoryVO> platformTree() {
        return categoryService.tree();
    }

    @Override
    @Transactional
    public Long create(Long merchantId, MerchantCategoryRequest req) {
        MerchantCategory c = new MerchantCategory();
        c.setMerchantId(merchantId);
        c.setName(req.getName());
        c.setIcon(req.getIcon());
        c.setSort(req.getSort() == null ? 0 : req.getSort());
        c.setStatus(1);

        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        if (parentId == 0L) {
            c.setParentId(0L);
            c.setLevel(1);
        } else {
            MerchantCategory parent = mustOwn(parentId, merchantId);
            if (!Integer.valueOf(1).equals(parent.getLevel())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "二级分类的父分类必须是一级");
            }
            c.setParentId(parentId);
            c.setLevel(2);
        }
        merchantCategoryMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional
    public void importFromPlatform(Long merchantId, MerchantCategoryImportRequest req) {
        boolean includeChildren = Boolean.TRUE.equals(req.getIncludeChildren());
        Set<Long> ids = new LinkedHashSet<>(req.getSourceCategoryIds());
        for (Long sourceId : ids) {
            Category source = categoryMapper.selectById(sourceId);
            if (source == null || !Integer.valueOf(1).equals(source.getStatus())) {
                continue;
            }
            if (Integer.valueOf(1).equals(source.getLevel())) {
                MerchantCategory root = importOne(merchantId, source, 0L);
                if (includeChildren) {
                    List<Category> children = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>()
                                    .eq(Category::getParentId, source.getId())
                                    .eq(Category::getStatus, 1)
                                    .orderByAsc(Category::getSort)
                                    .orderByAsc(Category::getId));
                    for (Category child : children) {
                        importOne(merchantId, child, root.getId());
                    }
                }
            } else if (Integer.valueOf(2).equals(source.getLevel())) {
                Category parent = categoryMapper.selectById(source.getParentId());
                if (parent == null || !Integer.valueOf(1).equals(parent.getStatus())) {
                    continue;
                }
                MerchantCategory merchantParent = importOne(merchantId, parent, 0L);
                importOne(merchantId, source, merchantParent.getId());
            }
        }
    }

    @Override
    @Transactional
    public void update(Long merchantId, Long id, MerchantCategoryRequest req) {
        MerchantCategory exist = mustOwn(id, merchantId);
        exist.setName(req.getName());
        exist.setIcon(req.getIcon());
        if (req.getSort() != null) {
            exist.setSort(req.getSort());
        }
        merchantCategoryMapper.updateById(exist);
    }

    @Override
    @Transactional
    public void setStatus(Long merchantId, Long id, int status) {
        MerchantCategory exist = mustOwn(id, merchantId);
        exist.setStatus(status == 1 ? 1 : 0);
        merchantCategoryMapper.updateById(exist);
    }

    @Override
    @Transactional
    public void delete(Long merchantId, Long id) {
        MerchantCategory exist = mustOwn(id, merchantId);
        if (Integer.valueOf(1).equals(exist.getLevel())) {
            Long children = merchantCategoryMapper.selectCount(
                    new LambdaQueryWrapper<MerchantCategory>()
                            .eq(MerchantCategory::getMerchantId, merchantId)
                            .eq(MerchantCategory::getParentId, id));
            if (children != null && children > 0) {
                throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
            }
        }
        Long products = productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getMerchantId, merchantId)
                        .eq(Product::getCategoryId, id));
        if (products != null && products > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        merchantCategoryMapper.deleteById(id);
    }

    @Override
    public void validateUsableCategory(Long merchantId, Long categoryId) {
        MerchantCategory c = mustOwn(categoryId, merchantId);
        if (!Integer.valueOf(1).equals(c.getStatus())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (!Integer.valueOf(2).equals(c.getLevel())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "请选择二级分类");
        }
        MerchantCategory parent = merchantCategoryMapper.selectById(c.getParentId());
        if (parent == null || !merchantId.equals(parent.getMerchantId()) || !Integer.valueOf(1).equals(parent.getStatus())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Override
    public List<Long> resolveCategoryScopeIds(Long merchantId, Long categoryId) {
        MerchantCategory category = merchantCategoryMapper.selectById(categoryId);
        if (category == null || !merchantId.equals(category.getMerchantId())) {
            return List.of();
        }
        Set<Long> ids = new HashSet<>();
        ids.add(category.getId());
        if (Integer.valueOf(1).equals(category.getLevel())) {
            merchantCategoryMapper.selectList(
                            new LambdaQueryWrapper<MerchantCategory>()
                                    .select(MerchantCategory::getId)
                                    .eq(MerchantCategory::getMerchantId, merchantId)
                                    .eq(MerchantCategory::getParentId, category.getId()))
                    .forEach(c -> ids.add(c.getId()));
        }
        return new ArrayList<>(ids);
    }

    @Override
    public List<Long> findMatchedCategoryIds(Long merchantId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        List<MerchantCategory> matched = merchantCategoryMapper.selectList(
                new LambdaQueryWrapper<MerchantCategory>()
                        .select(MerchantCategory::getId, MerchantCategory::getLevel)
                        .eq(MerchantCategory::getMerchantId, merchantId)
                        .like(MerchantCategory::getName, keyword));
        if (matched.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = new HashSet<>();
        List<Long> topIds = new ArrayList<>();
        for (MerchantCategory category : matched) {
            ids.add(category.getId());
            if (Integer.valueOf(1).equals(category.getLevel())) {
                topIds.add(category.getId());
            }
        }
        if (!topIds.isEmpty()) {
            merchantCategoryMapper.selectList(
                            new LambdaQueryWrapper<MerchantCategory>()
                                    .select(MerchantCategory::getId)
                                    .eq(MerchantCategory::getMerchantId, merchantId)
                                    .in(MerchantCategory::getParentId, topIds))
                    .forEach(c -> ids.add(c.getId()));
        }
        return new ArrayList<>(ids);
    }

    @Override
    public String getCategoryName(Long merchantId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        MerchantCategory c = merchantCategoryMapper.selectById(categoryId);
        if (c == null || (merchantId != null && !merchantId.equals(c.getMerchantId()))) {
            return null;
        }
        return c.getName();
    }

    @Override
    public Map<Long, String> getCategoryNames(Long merchantId, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<MerchantCategory> q = new LambdaQueryWrapper<MerchantCategory>()
                .select(MerchantCategory::getId, MerchantCategory::getName)
                .in(MerchantCategory::getId, categoryIds);
        if (merchantId != null) {
            q.eq(MerchantCategory::getMerchantId, merchantId);
        }
        Map<Long, String> result = new HashMap<>();
        merchantCategoryMapper.selectList(q).forEach(c -> result.put(c.getId(), c.getName()));
        return result;
    }

    private MerchantCategory importOne(Long merchantId, Category source, Long parentId) {
        MerchantCategory exist = findBySource(merchantId, source.getId());
        if (exist != null) {
            return exist;
        }
        MerchantCategory c = new MerchantCategory();
        c.setMerchantId(merchantId);
        c.setSourceCategoryId(source.getId());
        c.setParentId(parentId);
        c.setName(source.getName());
        c.setIcon(source.getIcon());
        c.setLevel(source.getLevel());
        c.setSort(source.getSort() == null ? 0 : source.getSort());
        c.setStatus(1);
        merchantCategoryMapper.insert(c);
        return c;
    }

    private MerchantCategory findBySource(Long merchantId, Long sourceCategoryId) {
        return merchantCategoryMapper.selectOne(
                new LambdaQueryWrapper<MerchantCategory>()
                        .eq(MerchantCategory::getMerchantId, merchantId)
                        .eq(MerchantCategory::getSourceCategoryId, sourceCategoryId)
                        .last("limit 1"));
    }

    private MerchantCategory mustOwn(Long id, Long merchantId) {
        MerchantCategory c = merchantCategoryMapper.selectById(id);
        if (c == null || !merchantId.equals(c.getMerchantId())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return c;
    }

    private List<MerchantCategoryVO> buildTree(List<MerchantCategory> all) {
        Map<Long, MerchantCategoryVO> idx = new HashMap<>();
        List<MerchantCategoryVO> roots = new ArrayList<>();
        for (MerchantCategory c : all) {
            MerchantCategoryVO vo = toVO(c);
            idx.put(c.getId(), vo);
        }
        for (MerchantCategory c : all) {
            MerchantCategoryVO vo = idx.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(vo);
            } else {
                MerchantCategoryVO parent = idx.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        Comparator<MerchantCategoryVO> bySort = Comparator
                .comparing(MerchantCategoryVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MerchantCategoryVO::getId);
        roots.sort(bySort);
        roots.forEach(r -> r.getChildren().sort(bySort));
        return roots;
    }

    private MerchantCategoryVO toVO(MerchantCategory c) {
        MerchantCategoryVO vo = new MerchantCategoryVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
