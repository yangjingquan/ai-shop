package com.shop.product.service;

import com.shop.product.dto.CategoryVO;
import com.shop.product.dto.MerchantCategoryImportRequest;
import com.shop.product.dto.MerchantCategoryRequest;
import com.shop.product.dto.MerchantCategoryVO;

import java.util.List;

public interface MerchantCategoryService {

    List<MerchantCategoryVO> tree(Long merchantId, boolean enabledOnly);

    List<CategoryVO> platformTree();

    Long create(Long merchantId, MerchantCategoryRequest req);

    void importFromPlatform(Long merchantId, MerchantCategoryImportRequest req);

    void update(Long merchantId, Long id, MerchantCategoryRequest req);

    void setStatus(Long merchantId, Long id, int status);

    void delete(Long merchantId, Long id);

    void validateUsableCategory(Long merchantId, Long categoryId);

    List<Long> resolveCategoryScopeIds(Long merchantId, Long categoryId);

    List<Long> findMatchedCategoryIds(Long merchantId, String keyword);

    String getCategoryName(Long merchantId, Long categoryId);
}
