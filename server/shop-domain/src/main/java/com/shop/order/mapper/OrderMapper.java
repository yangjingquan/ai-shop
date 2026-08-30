package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.order.entity.Order;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE product SET total_sales = total_sales + #{delta} WHERE id = #{productId}")
    void addTotalSales(@Param("productId") Long productId, @Param("delta") int delta);

    @Select("SELECT * FROM `order` WHERE status = 0 AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE) ORDER BY id ASC LIMIT #{limit}")
    List<Order> selectExpiredOrders(@Param("limit") int limit);

    @Update("UPDATE product_sku SET stock = stock + #{qty} WHERE id = #{skuId}")
    void releaseStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    @Update("UPDATE `order` SET status = 2, ship_company = #{shipCompany}, ship_no = #{shipNo}, ship_time = #{now}, updated_at = #{now} " +
            "WHERE order_no = #{orderNo} AND merchant_id = #{merchantId} AND deleted = 0 " +
            "AND ((order_type = 0 AND status = 1) OR (order_type = 1 AND status = 6))")
    int ship(@Param("merchantId") Long merchantId, @Param("orderNo") String orderNo,
             @Param("shipCompany") String shipCompany, @Param("shipNo") String shipNo,
             @Param("now") LocalDateTime now);

    @Update("UPDATE `order` SET status = 3, finish_time = #{now}, updated_at = #{now} WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 2 AND deleted = 0")
    int confirmReceive(@Param("userId") Long userId, @Param("orderNo") String orderNo, @Param("now") LocalDateTime now);

    @Update("UPDATE `order` SET ship_reminder_at = #{now}, updated_at = #{now} " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND deleted = 0 " +
            "AND status IN (1, 6) AND (ship_reminder_at IS NULL OR ship_reminder_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE))")
    int remindShip(@Param("userId") Long userId, @Param("orderNo") String orderNo, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM `order` WHERE status = 2 AND ship_time < DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY id ASC LIMIT #{limit}")
    List<Order> selectAutoReceiveOrders(@Param("limit") int limit);
}
