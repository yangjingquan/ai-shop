package com.shop.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.inventory.dto.InventoryTransactionVO;
import com.shop.inventory.entity.InventoryTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransaction> {

    @Select("""
            <script>
            SELECT it.id, it.product_id, it.sku_id, p.name AS product_name,
                   ps.sku_code, ps.spec_text, it.change_qty, it.stock_before,
                   it.stock_after, it.operation_type, it.reference_no, it.reason,
                   it.operator_id, it.created_at
            FROM inventory_transaction it
            JOIN product p ON p.id = it.product_id
            LEFT JOIN product_sku ps ON ps.id = it.sku_id
            WHERE it.merchant_id = #{merchantId} AND it.deleted = 0
            <if test="skuId != null">AND it.sku_id = #{skuId}</if>
            ORDER BY it.created_at DESC, it.id DESC
            </script>
            """)
    IPage<InventoryTransactionVO> selectMerchantPage(Page<?> page,
                                                       @Param("merchantId") Long merchantId,
                                                       @Param("skuId") Long skuId);
}
