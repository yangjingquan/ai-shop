package com.shop.presale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.presale.entity.PresaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PresaleOrderMapper extends BaseMapper<PresaleOrder> {
    @Select("SELECT * FROM presale_order WHERE (order_no = #{orderNo} OR deposit_order_no = #{orderNo} OR balance_order_no = #{orderNo}) AND deleted = 0 LIMIT 1 FOR UPDATE")
    PresaleOrder selectByAnyOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM presale_order WHERE (order_no = #{orderNo} OR deposit_order_no = #{orderNo} OR balance_order_no = #{orderNo}) AND user_id = #{userId} AND deleted = 0 LIMIT 1")
    PresaleOrder selectByAnyOrderNo(@Param("userId") Long userId, @Param("orderNo") String orderNo);

    @Select("SELECT * FROM presale_order WHERE user_id = #{userId} AND activity_id = #{activityId} AND presale_sku_id = #{presaleSkuId} AND stage IN (0, 1, 2, 3) AND deleted = 0")
    List<PresaleOrder> selectActiveByUserSku(@Param("userId") Long userId, @Param("activityId") Long activityId,
                                              @Param("presaleSkuId") Long presaleSkuId);

    @Select("SELECT * FROM presale_order WHERE stage = 1 AND balance_deadline < NOW() AND deleted = 0 ORDER BY balance_deadline ASC LIMIT #{limit}")
    List<PresaleOrder> selectExpiredWaitingBalance(@Param("limit") int limit);

    @Select("SELECT * FROM presale_order WHERE stage = 0 AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE) AND deleted = 0 ORDER BY created_at ASC LIMIT #{limit}")
    List<PresaleOrder> selectExpiredUnpaidDeposit(@Param("limit") int limit);
}
