package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT COUNT(*) FROM order_item oi JOIN `order` o ON o.id = oi.order_id "
            + "WHERE oi.product_id = #{productId} AND o.deleted = 0 AND o.status <> 4")
    long countActiveOrderReferences(@Param("productId") Long productId);
}
