package com.shop.product.service;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.product.dto.CategoryRequest;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.dto.ProductListVO;
import com.shop.product.dto.ProductSaveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    private static final Long M_A = 1001L;
    private static final Long M_B = 1002L;

    private Long createCategory() {
        return createCategory("M3T-类目-" + System.nanoTime());
    }

    private Long createCategory(String name) {
        return createCategory(name, 0L);
    }

    private Long createCategory(String name, Long parentId) {
        CategoryRequest c = new CategoryRequest();
        c.setName(name);
        c.setParentId(parentId);
        c.setSort(1);
        return categoryService.create(c);
    }

    private ProductSaveRequest sample(Long categoryId) {
        return sample(categoryId, "M3T 测试商品");
    }

    private ProductSaveRequest sample(Long categoryId, String name) {
        ProductSaveRequest r = new ProductSaveRequest();
        r.setName(name);
        r.setSubtitle("副标题");
        r.setCategoryId(categoryId);
        r.setMainImage("https://example.com/main.jpg");
        r.setImages(List.of("https://example.com/1.jpg"));
        r.setDescription("<p>详情</p>");

        ProductSaveRequest.SpecInput s1 = new ProductSaveRequest.SpecInput();
        s1.setName("颜色");
        s1.setValues(List.of("黑", "白"));
        ProductSaveRequest.SpecInput s2 = new ProductSaveRequest.SpecInput();
        s2.setName("存储");
        s2.setValues(List.of("256G", "512G"));
        r.setSpecs(List.of(s1, s2));

        ProductSaveRequest.SkuInput k1 = sku(List.of(0, 0), "7999.00", 10, "BLK-256");
        ProductSaveRequest.SkuInput k2 = sku(List.of(0, 1), "8999.00", 8, "BLK-512");
        ProductSaveRequest.SkuInput k3 = sku(List.of(1, 0), "7999.00", 6, null);
        ProductSaveRequest.SkuInput k4 = sku(List.of(1, 1), "8999.00", 5, null);
        r.setSkus(List.of(k1, k2, k3, k4));
        return r;
    }

    private ProductSaveRequest.SkuInput sku(List<Integer> idx, String price, int stock, String code) {
        ProductSaveRequest.SkuInput k = new ProductSaveRequest.SkuInput();
        k.setSpecValueIndexes(idx);
        k.setPrice(new BigDecimal(price));
        k.setStock(stock);
        k.setSkuCode(code);
        return k;
    }

    @Test
    void createWithSpecsAndSkus() {
        Long cid = createCategory();
        Long pid = productService.create(sample(cid), M_A);

        ProductDetailVO detail = productService.get(pid, M_A);
        assertEquals("M3T 测试商品", detail.getName());
        assertEquals(2, detail.getSpecs().size());
        assertEquals(4, detail.getSkus().size());

        // 冗余字段
        assertEquals(0, detail.getMinPrice().compareTo(new BigDecimal("7999.00")));
        assertEquals(0, detail.getMaxPrice().compareTo(new BigDecimal("8999.00")));
        assertEquals(29, detail.getTotalStock());

        // spec_text 包含 / 拼接
        boolean any = detail.getSkus().stream()
                .anyMatch(s -> s.getSpecText() != null && s.getSpecText().contains(" / "));
        assertTrue(any, "spec_text 应包含 ' / ' 拼接");
    }

    @Test
    void updateRebuildsSpecsAndSkus() {
        Long cid = createCategory();
        Long pid = productService.create(sample(cid), M_A);

        // 编辑：换成 1 规格 1 值 2 SKU
        ProductSaveRequest edit = new ProductSaveRequest();
        edit.setName("M3T 改名");
        edit.setCategoryId(cid);
        ProductSaveRequest.SpecInput s = new ProductSaveRequest.SpecInput();
        s.setName("颜色");
        s.setValues(List.of("金"));
        edit.setSpecs(List.of(s));
        edit.setSkus(List.of(
                sku(List.of(0), "6999.00", 20, null),
                sku(List.of(0), "7999.00", 15, "DUP")
        ));
        productService.update(pid, edit, M_A);

        ProductDetailVO detail = productService.get(pid, M_A);
        assertEquals("M3T 改名", detail.getName());
        assertEquals(1, detail.getSpecs().size());
        assertEquals(1, detail.getSpecs().get(0).getValues().size());
        assertEquals(2, detail.getSkus().size());
        assertEquals(0, detail.getMinPrice().compareTo(new BigDecimal("6999.00")));
        assertEquals(0, detail.getMaxPrice().compareTo(new BigDecimal("7999.00")));
        assertEquals(35, detail.getTotalStock());
    }

    @Test
    void crossMerchantBlocked() {
        Long cid = createCategory();
        Long pid = productService.create(sample(cid), M_A);

        // B 商家 get → PRODUCT_NOT_FOUND
        BusinessException g = assertThrows(BusinessException.class,
                () -> productService.get(pid, M_B));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND.getCode(), g.getCode());

        // B 商家 update → 同上
        BusinessException u = assertThrows(BusinessException.class,
                () -> productService.update(pid, sample(cid), M_B));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND.getCode(), u.getCode());

        // B 商家 setStatus / delete → 同上
        BusinessException s = assertThrows(BusinessException.class,
                () -> productService.setStatus(pid, 1, M_B));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND.getCode(), s.getCode());

        BusinessException d = assertThrows(BusinessException.class,
                () -> productService.delete(pid, M_B));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND.getCode(), d.getCode());
    }

    @Test
    void publicPageSearchesProductNameOrCategoryName() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Long phoneTopCategoryId = createCategory("M3T数码一级分类" + token);
        Long foodTopCategoryId = createCategory("M3T食品一级分类" + token);
        Long phoneCategoryId = createCategory("M3T手机二级分类" + token, phoneTopCategoryId);
        Long foodCategoryId = createCategory("M3T食品二级分类" + token, foodTopCategoryId);
        Long phoneProductId = productService.create(sample(phoneCategoryId, "M3T旗舰机" + token), M_A);
        Long foodProductId = productService.create(sample(foodCategoryId, "M3T零食" + token), M_A);
        productService.setStatus(phoneProductId, 1, M_A);
        productService.setStatus(foodProductId, 1, M_A);

        PageResult<ProductListVO> byProductName = productService.page(1, 20, null, null, "旗舰", null);
        assertTrue(byProductName.getList().stream().anyMatch(v -> v.getId().equals(phoneProductId)));
        assertTrue(byProductName.getList().stream().noneMatch(v -> v.getId().equals(foodProductId)));

        PageResult<ProductListVO> byCategoryName = productService.page(1, 20, null, null, "食品二级分类" + token, null);
        assertTrue(byCategoryName.getList().stream().anyMatch(v -> v.getId().equals(foodProductId)));
        assertTrue(byCategoryName.getList().stream().noneMatch(v -> v.getId().equals(phoneProductId)));

        PageResult<ProductListVO> byTopCategoryName = productService.page(1, 20, null, null, "数码一级分类" + token, null);
        assertTrue(byTopCategoryName.getList().stream().anyMatch(v -> v.getId().equals(phoneProductId)));
        assertTrue(byTopCategoryName.getList().stream().noneMatch(v -> v.getId().equals(foodProductId)));
    }

    @Test
    void publicPageTopCategoryIncludesSelfAndChildren() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Long topCategoryId = createCategory("M3T一级筛选" + token);
        Long childCategoryId = createCategory("M3T二级筛选" + token, topCategoryId);
        Long otherTopCategoryId = createCategory("M3T其他一级" + token);
        Long topProductId = productService.create(sample(topCategoryId, "M3T一级商品" + token), M_A);
        Long childProductId = productService.create(sample(childCategoryId, "M3T二级商品" + token), M_A);
        Long otherProductId = productService.create(sample(otherTopCategoryId, "M3T其他商品" + token), M_A);
        productService.setStatus(topProductId, 1, M_A);
        productService.setStatus(childProductId, 1, M_A);
        productService.setStatus(otherProductId, 1, M_A);

        PageResult<ProductListVO> byTopCategory = productService.page(1, 20, null, topCategoryId, null, null);
        assertTrue(byTopCategory.getList().stream().anyMatch(v -> v.getId().equals(topProductId)));
        assertTrue(byTopCategory.getList().stream().anyMatch(v -> v.getId().equals(childProductId)));
        assertTrue(byTopCategory.getList().stream().noneMatch(v -> v.getId().equals(otherProductId)));
    }

    @Test
    void publicPageCategoryAndKeywordShouldBeIntersection() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Long topCategoryId = createCategory("M3T交集一级" + token);
        Long childCategoryId = createCategory("M3T交集二级" + token, topCategoryId);
        Long otherCategoryId = createCategory("M3T跨类二级" + token);
        Long insideProductId = productService.create(sample(childCategoryId, "M3T苹果手机" + token), M_A);
        Long outsideProductId = productService.create(sample(otherCategoryId, "M3T苹果平板" + token), M_A);
        productService.setStatus(insideProductId, 1, M_A);
        productService.setStatus(outsideProductId, 1, M_A);

        PageResult<ProductListVO> result = productService.page(1, 20, null, topCategoryId, "苹果", null);
        assertTrue(result.getList().stream().anyMatch(v -> v.getId().equals(insideProductId)));
        assertTrue(result.getList().stream().noneMatch(v -> v.getId().equals(outsideProductId)));
    }

    @Test
    void publicPageOnlyOnSale() {
        Long cid = createCategory();
        Long pid = productService.create(sample(cid), M_A);

        // 默认下架，public 看不到
        PageResult<ProductListVO> p1 = productService.page(1, 20, null, cid, null, null);
        assertTrue(p1.getList().stream().noneMatch(v -> v.getId().equals(pid)),
                "默认下架商品不应在公共列表");

        // public detail 也 404
        BusinessException g = assertThrows(BusinessException.class,
                () -> productService.get(pid, null));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND.getCode(), g.getCode());

        // 上架后能看到
        productService.setStatus(pid, 1, M_A);
        PageResult<ProductListVO> p2 = productService.page(1, 20, null, cid, null, null);
        assertTrue(p2.getList().stream().anyMatch(v -> v.getId().equals(pid)),
                "上架后商品应在公共列表");

        ProductDetailVO detail = productService.get(pid, null);
        assertEquals(pid, detail.getId());
    }
}
