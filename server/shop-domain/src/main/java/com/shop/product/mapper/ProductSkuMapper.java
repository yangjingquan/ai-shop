package com.shop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.inventory.dto.InventorySkuVO;
import com.shop.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /** 乐观锁扣库存，返回受影响行数（0 表示库存不足或行不存在） */
    @Update("UPDATE product_sku SET stock = stock - #{qty} WHERE id = #{skuId} AND deleted = 0 AND active = 1 AND stock >= #{qty}")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 带原库存条件的安全调整，避免并发覆盖其他库存变更。 */
    @Update("UPDATE product_sku SET stock = #{stockAfter} WHERE id = #{skuId} AND deleted = 0 AND active = 1 AND stock = #{stockBefore}")
    int adjustStock(@Param("skuId") Long skuId,
                    @Param("stockBefore") int stockBefore,
                    @Param("stockAfter") int stockAfter);

    @Select("""
            <script>
            SELECT ps.id AS sku_id, ps.product_id, p.name AS product_name,
                   p.main_image, ps.sku_code, ps.spec_text, ps.stock
            FROM product_sku ps
            JOIN product p ON p.id = ps.product_id
            WHERE p.merchant_id = #{merchantId}
              AND p.deleted = 0 AND ps.deleted = 0 AND ps.active = 1
            <if test="keyword != null and keyword != ''">
              AND (p.name LIKE CONCAT('%', #{keyword}, '%')
                   OR ps.sku_code LIKE CONCAT('%', #{keyword}, '%')
                   OR ps.spec_text LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="lowStockOnly">
              AND ps.stock &lt;= #{threshold}
            </if>
            ORDER BY ps.stock ASC, ps.id DESC
            </script>
            """)
    IPage<InventorySkuVO> selectMerchantInventoryPage(Page<?> page,
                                                       @Param("merchantId") Long merchantId,
                                                       @Param("keyword") String keyword,
                                                       @Param("lowStockOnly") boolean lowStockOnly,
                                                       @Param("threshold") int threshold);
}
