package com.shop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /** 乐观锁扣库存，返回受影响行数（0 表示库存不足或行不存在） */
    @Update("UPDATE product_sku SET stock = stock - #{qty} WHERE id = #{skuId} AND deleted = 0 AND stock >= #{qty}")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") int qty);
}
