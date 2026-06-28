package com.shop.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.product.dto.CategoryRequest;
import com.shop.product.dto.CategoryVO;
import com.shop.product.entity.Category;
import com.shop.product.mapper.CategoryMapper;
import com.shop.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> tree() {
        return buildTree(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)));
    }

    @Override
    public List<CategoryVO> adminTree() {
        return buildTree(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }

    private List<CategoryVO> buildTree(List<Category> all) {
        Map<Long, CategoryVO> idx = new HashMap<>();
        List<CategoryVO> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryVO vo = toVO(c);
            idx.put(c.getId(), vo);
        }
        for (Category c : all) {
            CategoryVO vo = idx.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(vo);
            } else {
                CategoryVO parent = idx.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        Comparator<CategoryVO> bySort = Comparator
                .comparing(CategoryVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryVO::getId);
        roots.sort(bySort);
        roots.forEach(r -> r.getChildren().sort(bySort));
        return roots;
    }

    private CategoryVO toVO(Category c) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    @Override
    @Transactional
    public Long create(CategoryRequest req) {
        Category c = new Category();
        c.setName(req.getName());
        c.setIcon(req.getIcon());
        c.setSort(req.getSort() == null ? 0 : req.getSort());
        c.setStatus(1);

        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        if (parentId == 0L) {
            c.setParentId(0L);
            c.setLevel(1);
        } else {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            if (parent.getLevel() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "二级分类的父分类必须是一级");
            }
            c.setParentId(parentId);
            c.setLevel(2);
        }
        categoryMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CategoryRequest req) {
        Category exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        // M3 简化：层级不允许改（避免破坏父子关系）
        exist.setName(req.getName());
        exist.setIcon(req.getIcon());
        if (req.getSort() != null) {
            exist.setSort(req.getSort());
        }
        categoryMapper.updateById(exist);
    }

    @Override
    @Transactional
    public void setStatus(Long id, int status) {
        Category exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        exist.setStatus(status == 1 ? 1 : 0);
        categoryMapper.updateById(exist);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        // 检查无子分类（一级被删时；二级直接允许）
        if (exist.getLevel() == 1) {
            Long children = categoryMapper.selectCount(
                    new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
            if (children != null && children > 0) {
                throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
            }
        }
        // TODO M4：检查无关联商品
        categoryMapper.deleteById(id);
    }
}
