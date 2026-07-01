package com.shop.product.service;

import com.shop.product.dto.CategoryRequest;
import com.shop.product.dto.CategoryVO;

import java.util.List;

public interface CategoryService {

    /** 公开 tree，只返回 status=1 */
    List<CategoryVO> tree();

    /** admin tree，含禁用 */
    List<CategoryVO> adminTree();

    Long create(CategoryRequest req);

    void update(Long id, CategoryRequest req);

    void setStatus(Long id, int status);

    void delete(Long id);
}
