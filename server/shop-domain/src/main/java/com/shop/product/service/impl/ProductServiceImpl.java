package com.shop.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.config.XssSanitizer;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.dto.ProductSaveRequest;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.entity.ProductSpec;
import com.shop.product.entity.ProductSpecValue;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.product.mapper.ProductSpecMapper;
import com.shop.product.mapper.ProductSpecValueMapper;
import com.shop.product.service.MerchantCategoryService;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int SKU_LIMIT = 100;

    private final ProductMapper productMapper;
    private final ProductSpecMapper specMapper;
    private final ProductSpecValueMapper specValueMapper;
    private final ProductSkuMapper skuMapper;
    private final MerchantCategoryService merchantCategoryService;

    @Override
    @Transactional
    public Long create(ProductSaveRequest req, Long merchantId) {
        merchantCategoryService.validateUsableCategory(merchantId, req.getCategoryId());
        validateSpecs(req.getSpecs());
        validateSkus(req.getSkus(), req.getSpecs());

        Product p = new Product();
        p.setMerchantId(merchantId);
        p.setCategoryId(req.getCategoryId());
        p.setName(req.getName());
        p.setSubtitle(req.getSubtitle());
        p.setMainImage(req.getMainImage());
        p.setImages(req.getImages());
        p.setDescription(XssSanitizer.sanitize(req.getDescription()));
        p.setMinPrice(BigDecimal.ZERO);
        p.setMaxPrice(BigDecimal.ZERO);
        p.setMinOriginalPrice(null);
        p.setMaxOriginalPrice(null);
        p.setTotalStock(0);
        p.setTotalSales(0);
        p.setStatus(0);
        p.setIsRecommend(normalizeFlag(req.getIsRecommend()));
        p.setSort(0);
        productMapper.insert(p);

        persistSpecsAndSkus(p.getId(), req);
        recalcProduct(p.getId());
        return p.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ProductSaveRequest req, Long merchantId) {
        Product p = mustOwn(id, merchantId);
        merchantCategoryService.validateUsableCategory(merchantId, req.getCategoryId());
        validateSpecs(req.getSpecs());
        validateSkus(req.getSkus(), req.getSpecs());

        p.setCategoryId(req.getCategoryId());
        p.setName(req.getName());
        p.setSubtitle(req.getSubtitle());
        p.setMainImage(req.getMainImage());
        p.setImages(req.getImages());
        p.setDescription(XssSanitizer.sanitize(req.getDescription()));
        p.setIsRecommend(normalizeFlag(req.getIsRecommend()));
        productMapper.updateById(p);

        // 先删后插
        deleteSpecsAndSkus(id);
        persistSpecsAndSkus(id, req);
        recalcProduct(id);
    }

    @Override
    public ProductDetailVO get(Long id, Long merchantId) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (merchantId != null && !merchantId.equals(p.getMerchantId())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 公共视角（merchantId==null）只能看上架商品
        if (merchantId == null && (p.getStatus() == null || p.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(p.getId());
        vo.setMerchantId(p.getMerchantId());
        vo.setCategoryId(p.getCategoryId());
        vo.setCategoryName(merchantCategoryService.getCategoryName(p.getMerchantId(), p.getCategoryId()));
        vo.setName(p.getName());
        vo.setSubtitle(p.getSubtitle());
        vo.setMainImage(p.getMainImage());
        vo.setImages(p.getImages());
        vo.setDescription(p.getDescription());
        vo.setMinPrice(p.getMinPrice());
        vo.setMaxPrice(p.getMaxPrice());
        BigDecimal detailMinOriginalPrice = p.getMinOriginalPrice();
        BigDecimal detailMaxOriginalPrice = p.getMaxOriginalPrice();
        vo.setTotalStock(p.getTotalStock());
        vo.setTotalSales(p.getTotalSales());
        vo.setStatus(p.getStatus());
        vo.setIsRecommend(p.getIsRecommend());
        vo.setSort(p.getSort());

        List<ProductSpec> specs = specMapper.selectList(
                new LambdaQueryWrapper<ProductSpec>()
                        .eq(ProductSpec::getProductId, id)
                        .orderByAsc(ProductSpec::getSort)
                        .orderByAsc(ProductSpec::getId));
        List<Long> specIds = specs.stream().map(ProductSpec::getId).collect(Collectors.toList());

        Map<Long, List<ProductSpecValue>> valuesBySpec = new HashMap<>();
        Map<Long, ProductSpecValue> valueById = new HashMap<>();
        if (!specIds.isEmpty()) {
            List<ProductSpecValue> values = specValueMapper.selectList(
                    new LambdaQueryWrapper<ProductSpecValue>()
                            .in(ProductSpecValue::getSpecId, specIds)
                            .orderByAsc(ProductSpecValue::getSort)
                            .orderByAsc(ProductSpecValue::getId));
            for (ProductSpecValue v : values) {
                valuesBySpec.computeIfAbsent(v.getSpecId(), k -> new ArrayList<>()).add(v);
                valueById.put(v.getId(), v);
            }
        }

        for (ProductSpec s : specs) {
            ProductDetailVO.SpecVO sv = new ProductDetailVO.SpecVO();
            sv.setId(s.getId());
            sv.setName(s.getName());
            sv.setSort(s.getSort());
            for (ProductSpecValue v : valuesBySpec.getOrDefault(s.getId(), List.of())) {
                ProductDetailVO.SpecValueVO vv = new ProductDetailVO.SpecValueVO();
                vv.setId(v.getId());
                vv.setValue(v.getValue());
                vv.setSort(v.getSort());
                sv.getValues().add(vv);
            }
            vo.getSpecs().add(sv);
        }

        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
                        .orderByAsc(ProductSku::getId));
        for (ProductSku sk : skus) {
            ProductDetailVO.SkuVO svo = new ProductDetailVO.SkuVO();
            svo.setId(sk.getId());
            svo.setSkuCode(sk.getSkuCode());
            // JacksonTypeHandler 把 JSON 数字默认反序列化为 Integer，这里强制规范为 Long，避免 Jackson LongSerializer 强转报错
            List<Long> ids = new ArrayList<>();
            if (sk.getSpecValueIds() != null) {
                for (Object n : sk.getSpecValueIds()) {
                    ids.add(((Number) n).longValue());
                }
            }
            svo.setSpecValueIds(ids);
            svo.setSpecText(sk.getSpecText());
            svo.setPrice(sk.getPrice());
            svo.setOriginalPrice(sk.getOriginalPrice());
            if (sk.getOriginalPrice() != null) {
                if (detailMinOriginalPrice == null || sk.getOriginalPrice().compareTo(detailMinOriginalPrice) < 0) {
                    detailMinOriginalPrice = sk.getOriginalPrice();
                }
                if (detailMaxOriginalPrice == null || sk.getOriginalPrice().compareTo(detailMaxOriginalPrice) > 0) {
                    detailMaxOriginalPrice = sk.getOriginalPrice();
                }
            }
            svo.setStock(sk.getStock());
            svo.setImage(sk.getImage());
            vo.getSkus().add(svo);
        }
        vo.setMinOriginalPrice(detailMinOriginalPrice == null ? BigDecimal.ZERO : detailMinOriginalPrice);
        vo.setMaxOriginalPrice(detailMaxOriginalPrice == null ? BigDecimal.ZERO : detailMaxOriginalPrice);
        return vo;
    }

    @Override
    public ProductDetailVO publicGet(Long id, Long merchantId) {
        ProductDetailVO vo = get(id, merchantId);
        if (vo.getStatus() == null || vo.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return vo;
    }

    @Override
    public PageResult<ProductListVO> publicPage(int page, int size, Long merchantId, Long categoryId,
                                                String keyword, Integer isRecommend) {
        return page(page, size, merchantId, categoryId, keyword, 1, isRecommend);
    }

    @Override
    public PageResult<ProductListVO> page(int page, int size, Long merchantId, Long categoryId,
                                          String keyword, Integer status, Integer isRecommend) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        if (merchantId != null) {
            q.eq(Product::getMerchantId, merchantId);
        }
        if (categoryId != null) {
            List<Long> scopeCategoryIds = merchantCategoryService.resolveCategoryScopeIds(merchantId, categoryId);
            if (scopeCategoryIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            q.in(Product::getCategoryId, scopeCategoryIds);
        }
        if (status != null) {
            q.eq(Product::getStatus, status);
        }
        if (isRecommend != null) {
            q.eq(Product::getIsRecommend, normalizeFlag(isRecommend));
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> matchedCategoryIds = merchantCategoryService.findMatchedCategoryIds(merchantId, kw);
            q.and(w -> {
                w.like(Product::getName, kw);
                if (!matchedCategoryIds.isEmpty()) {
                    w.or().in(Product::getCategoryId, matchedCategoryIds);
                }
            });
        }
        if (Integer.valueOf(1).equals(isRecommend)) {
            q.orderByAsc(Product::getSort).orderByDesc(Product::getId);
        } else {
            q.orderByDesc(Product::getSort).orderByDesc(Product::getId);
        }

        IPage<Product> pageReq = new Page<>(page, size);
        IPage<Product> result = productMapper.selectPage(pageReq, q);

        Map<Long, String> catNames = new HashMap<>();
        result.getRecords().stream()
                .map(Product::getCategoryId)
                .distinct()
                .forEach(id -> catNames.put(id, merchantCategoryService.getCategoryName(merchantId, id)));

        List<ProductListVO> list = result.getRecords().stream().map(p -> {
            ProductListVO v = new ProductListVO();
            v.setId(p.getId());
            v.setMerchantId(p.getMerchantId());
            v.setName(p.getName());
            v.setMainImage(p.getMainImage());
            v.setMinPrice(p.getMinPrice());
            v.setMaxPrice(p.getMaxPrice());
            v.setMinOriginalPrice(p.getMinOriginalPrice());
            v.setMaxOriginalPrice(p.getMaxOriginalPrice());
            v.setTotalStock(p.getTotalStock());
            v.setTotalSales(p.getTotalSales());
            v.setStatus(p.getStatus());
            v.setIsRecommend(p.getIsRecommend());
            v.setCategoryId(p.getCategoryId());
            v.setCategoryName(catNames.get(p.getCategoryId()));
            return v;
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void setStatus(Long id, int status, Long merchantId) {
        Product p = mustOwn(id, merchantId);
        p.setStatus(status == 1 ? 1 : 0);
        productMapper.updateById(p);
    }

    @Override
    @Transactional
    public void delete(Long id, Long merchantId) {
        mustOwn(id, merchantId);
        deleteSpecsAndSkus(id);
        productMapper.deleteById(id);
    }

    // ============== private ==============

    private int normalizeFlag(Integer flag) {
        return Integer.valueOf(1).equals(flag) ? 1 : 0;
    }

    private Product mustOwn(Long id, Long merchantId) {
        Product p = productMapper.selectById(id);
        if (p == null || (merchantId != null && !merchantId.equals(p.getMerchantId()))) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return p;
    }

    private void validateSpecs(List<ProductSaveRequest.SpecInput> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SPEC);
        }
        Set<String> names = new HashSet<>();
        for (ProductSaveRequest.SpecInput s : specs) {
            if (!StringUtils.hasText(s.getName()) || s.getValues() == null || s.getValues().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_SPEC);
            }
            if (!names.add(s.getName())) {
                throw new BusinessException(ErrorCode.INVALID_SPEC);
            }
        }
    }

    private void validateSkus(List<ProductSaveRequest.SkuInput> skus, List<ProductSaveRequest.SpecInput> specs) {
        if (skus == null || skus.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SPEC);
        }
        if (skus.size() > SKU_LIMIT) {
            throw new BusinessException(ErrorCode.SKU_LIMIT_EXCEEDED);
        }
        for (ProductSaveRequest.SkuInput sku : skus) {
            if (sku.getOriginalPrice() != null && sku.getPrice() != null
                    && sku.getOriginalPrice().compareTo(sku.getPrice()) < 0) {
                throw new BusinessException(ErrorCode.INVALID_SPEC);
            }
            if (sku.getSpecValueIndexes() == null || sku.getSpecValueIndexes().size() != specs.size()) {
                throw new BusinessException(ErrorCode.INVALID_SPEC);
            }
            for (int i = 0; i < specs.size(); i++) {
                int idx = sku.getSpecValueIndexes().get(i);
                if (idx < 0 || idx >= specs.get(i).getValues().size()) {
                    throw new BusinessException(ErrorCode.INVALID_SPEC);
                }
            }
        }
    }

    private void persistSpecsAndSkus(Long productId, ProductSaveRequest req) {
        // specs + values
        List<List<Long>> valueIdsBySpec = new ArrayList<>();
        List<List<String>> valueTextsBySpec = new ArrayList<>();
        for (int i = 0; i < req.getSpecs().size(); i++) {
            ProductSaveRequest.SpecInput s = req.getSpecs().get(i);
            ProductSpec spec = new ProductSpec();
            spec.setProductId(productId);
            spec.setName(s.getName());
            spec.setSort(i);
            specMapper.insert(spec);

            List<Long> ids = new ArrayList<>();
            List<String> texts = new ArrayList<>();
            for (int j = 0; j < s.getValues().size(); j++) {
                ProductSpecValue v = new ProductSpecValue();
                v.setSpecId(spec.getId());
                v.setValue(s.getValues().get(j));
                v.setSort(j);
                specValueMapper.insert(v);
                ids.add(v.getId());
                texts.add(s.getValues().get(j));
            }
            valueIdsBySpec.add(ids);
            valueTextsBySpec.add(texts);
        }

        // skus
        for (ProductSaveRequest.SkuInput sku : req.getSkus()) {
            List<Long> specValueIds = new ArrayList<>();
            List<String> specTexts = new ArrayList<>();
            for (int i = 0; i < sku.getSpecValueIndexes().size(); i++) {
                int idx = sku.getSpecValueIndexes().get(i);
                specValueIds.add(valueIdsBySpec.get(i).get(idx));
                specTexts.add(valueTextsBySpec.get(i).get(idx));
            }
            ProductSku entity = new ProductSku();
            entity.setProductId(productId);
            entity.setSkuCode(sku.getSkuCode() == null ? "" : sku.getSkuCode());
            entity.setSpecValueIds(specValueIds);
            entity.setSpecText(String.join(" / ", specTexts));
            entity.setPrice(sku.getPrice());
            entity.setOriginalPrice(sku.getOriginalPrice());
            entity.setStock(sku.getStock());
            entity.setImage(sku.getImage() == null ? "" : sku.getImage());
            skuMapper.insert(entity);
        }
    }

    private void deleteSpecsAndSkus(Long productId) {
        List<ProductSpec> oldSpecs = specMapper.selectList(
                new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, productId));
        if (!oldSpecs.isEmpty()) {
            List<Long> specIds = oldSpecs.stream().map(ProductSpec::getId).collect(Collectors.toList());
            specValueMapper.delete(new LambdaQueryWrapper<ProductSpecValue>()
                    .in(ProductSpecValue::getSpecId, specIds));
            specMapper.delete(new LambdaQueryWrapper<ProductSpec>()
                    .in(ProductSpec::getId, specIds));
        }
        skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
    }

    @Override
    public void recalcProduct(Long productId) {
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal minOriginal = null;
        BigDecimal maxOriginal = null;
        int total = 0;
        if (!skus.isEmpty()) {
            min = skus.get(0).getPrice();
            max = skus.get(0).getPrice();
            for (ProductSku s : skus) {
                if (s.getPrice().compareTo(min) < 0) min = s.getPrice();
                if (s.getPrice().compareTo(max) > 0) max = s.getPrice();
                BigDecimal original = s.getOriginalPrice();
                if (original != null) {
                    if (minOriginal == null || original.compareTo(minOriginal) < 0) minOriginal = original;
                    if (maxOriginal == null || original.compareTo(maxOriginal) > 0) maxOriginal = original;
                }
                total += s.getStock() == null ? 0 : s.getStock();
            }
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getMinPrice, min)
                .set(Product::getMaxPrice, max)
                .set(Product::getMinOriginalPrice, minOriginal)
                .set(Product::getMaxOriginalPrice, maxOriginal)
                .set(Product::getTotalStock, total));
    }
}
