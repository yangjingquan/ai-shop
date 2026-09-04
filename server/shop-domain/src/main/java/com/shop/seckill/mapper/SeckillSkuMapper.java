package com.shop.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.seckill.entity.SeckillSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeckillSkuMapper extends BaseMapper<SeckillSku> {
    @Select("SELECT * FROM seckill_sku WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    SeckillSku selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE seckill_sku SET activity_stock = activity_stock - #{quantity} " +
            "WHERE id = #{id} AND deleted = 0 AND activity_stock >= #{quantity}")
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE seckill_sku SET activity_stock = activity_stock + #{quantity} " +
            "WHERE id = #{id} AND deleted = 0")
    int releaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE seckill_sku SET sold_count = sold_count + #{quantity} " +
            "WHERE id = #{id} AND deleted = 0")
    int addSoldCount(@Param("id") Long id, @Param("quantity") int quantity);
}
