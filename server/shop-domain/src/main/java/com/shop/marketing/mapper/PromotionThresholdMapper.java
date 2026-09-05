package com.shop.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.marketing.entity.PromotionThreshold;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionThresholdMapper extends BaseMapper<PromotionThreshold> {
    /** Threshold rows are replacement-only configuration and do not need soft-delete history. */
    @Delete("DELETE FROM promotion_threshold WHERE activity_id = #{activityId}")
    int purgeByActivityId(@Param("activityId") Long activityId);
}
