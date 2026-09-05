package com.shop.points.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.points.entity.PointsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
@Mapper public interface PointsProductMapper extends BaseMapper<PointsProduct> {
 @Update("UPDATE points_product SET stock = stock - #{qty} WHERE id = #{id} AND deleted = 0 AND status = 1 AND stock >= #{qty}") int deductStock(@Param("id") Long id, @Param("qty") int qty);
}
