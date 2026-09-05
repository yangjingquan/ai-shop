package com.shop.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.marketing.entity.PromotionActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PromotionActivityMapper extends BaseMapper<PromotionActivity> {
    @Update("UPDATE promotion_activity SET reserved_order_count = reserved_order_count + 1, reserved_budget = reserved_budget + #{discount}, updated_at = NOW() " +
            "WHERE id = #{id} AND deleted = 0 AND status = 1 AND start_at <= NOW() AND end_at > NOW() " +
            "AND (max_order_count IS NULL OR paid_order_count + reserved_order_count < max_order_count) " +
            "AND (budget_amount IS NULL OR paid_budget + reserved_budget + #{discount} <= budget_amount)")
    int reserve(@Param("id") Long id, @Param("discount") BigDecimal discount);

    @Update("UPDATE promotion_activity SET reserved_order_count = GREATEST(0, reserved_order_count - 1), " +
            "reserved_budget = GREATEST(0, reserved_budget - #{discount}), paid_order_count = paid_order_count + 1, " +
            "paid_budget = paid_budget + #{discount}, updated_at = NOW() WHERE id = #{id}")
    int markPaid(@Param("id") Long id, @Param("discount") BigDecimal discount);

    @Update("UPDATE promotion_activity SET reserved_order_count = GREATEST(0, reserved_order_count - 1), " +
            "reserved_budget = GREATEST(0, reserved_budget - #{discount}), updated_at = NOW() WHERE id = #{id}")
    int release(@Param("id") Long id, @Param("discount") BigDecimal discount);
}
