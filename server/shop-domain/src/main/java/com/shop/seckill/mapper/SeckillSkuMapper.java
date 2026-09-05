package com.shop.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.seckill.entity.SeckillSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeckillSkuMapper extends BaseMapper<SeckillSku> {
    @Select("<script>SELECT COUNT(*) FROM seckill_sku ss JOIN seckill_session s ON s.id = ss.session_id AND s.deleted = 0 JOIN seckill_activity a ON a.id = s.activity_id AND a.deleted = 0 WHERE ss.merchant_id = #{merchantId} AND ss.deleted = 0 AND a.status = 1 AND s.start_at &lt;= NOW() AND s.end_at &gt;= NOW() AND ss.product_id IN <foreach collection='productIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    long countActiveByProducts(@Param("merchantId") Long merchantId, @Param("productIds") List<Long> productIds);
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
