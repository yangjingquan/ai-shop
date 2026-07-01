package com.shop.product.service;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.product.dto.CategoryRequest;
import com.shop.product.dto.CategoryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    private CategoryRequest req(String name, Long parentId, Integer sort) {
        CategoryRequest r = new CategoryRequest();
        r.setName(name);
        r.setParentId(parentId);
        r.setSort(sort);
        return r;
    }

    @Test
    void createOneAndTwoLevel() {
        Long rootId = categoryService.create(req("M3T-数码", 0L, 1));
        Long childId = categoryService.create(req("M3T-手机", rootId, 1));
        assertNotNull(rootId);
        assertNotNull(childId);

        List<CategoryVO> tree = categoryService.tree();
        CategoryVO root = tree.stream().filter(c -> c.getId().equals(rootId)).findFirst().orElse(null);
        assertNotNull(root);
        assertEquals(1, root.getLevel());
        assertEquals(1, root.getChildren().size());
        assertEquals(childId, root.getChildren().get(0).getId());
        assertEquals(2, root.getChildren().get(0).getLevel());
    }

    @Test
    void deleteParentWithChildrenThrows() {
        Long rootId = categoryService.create(req("M3T-家电", 0L, 1));
        categoryService.create(req("M3T-空调", rootId, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.delete(rootId));
        assertEquals(ErrorCode.CATEGORY_HAS_CHILDREN.getCode(), ex.getCode());
    }

    @Test
    void disabledNotInPublicTree() {
        Long rootId = categoryService.create(req("M3T-服饰", 0L, 1));
        categoryService.setStatus(rootId, 0);

        boolean inPublic = categoryService.tree().stream()
                .anyMatch(c -> c.getId().equals(rootId));
        assertFalse(inPublic, "禁用的分类不应出现在 public tree");

        boolean inAdmin = categoryService.adminTree().stream()
                .anyMatch(c -> c.getId().equals(rootId));
        assertTrue(inAdmin, "禁用的分类应出现在 admin tree");
    }
}
