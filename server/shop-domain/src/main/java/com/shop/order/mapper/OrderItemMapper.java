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

    @Select("SELECT oi.* FROM order_item oi JOIN `order` o ON o.id = oi.order_id "
            + "WHERE oi.id = #{itemId} AND o.user_id = #{userId} AND o.merchant_id = #{merchantId} "
            + "AND o.status = 3 AND o.deleted = 0 LIMIT 1")
    OrderItem selectReviewable(@Param("itemId") Long itemId, @Param("userId") Long userId, @Param("merchantId") Long merchantId);

    @Select("SELECT COUNT(*) FROM order_item oi JOIN `order` o ON o.id = oi.order_id "
            + "WHERE oi.product_id = #{productId} AND o.user_id = #{userId} AND o.merchant_id = #{merchantId} "
            + "AND o.pay_time IS NOT NULL AND o.deleted = 0")
    long countPaidProduct(@Param("productId") Long productId, @Param("userId") Long userId, @Param("merchantId") Long merchantId);
}
