package com.shop.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.lottery.entity.LotteryPrize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LotteryPrizeMapper extends BaseMapper<LotteryPrize> {
    @Update("UPDATE lottery_prize SET remaining_stock = remaining_stock - 1 WHERE id = #{id} AND deleted = 0 AND status = 1 AND (remaining_stock > 0 OR total_stock = 0)")
    int deductStock(@Param("id") Long id);
}
