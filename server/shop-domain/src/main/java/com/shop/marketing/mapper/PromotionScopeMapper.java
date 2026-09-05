package com.shop.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.marketing.entity.PromotionScope;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionScopeMapper extends BaseMapper<PromotionScope> {
    /**
     * Scope rows are replacement-only configuration. Physically remove prior rows so
     * the logical-delete value cannot collide with uk_promotion_scope on later saves.
     */
    @Delete("DELETE FROM promotion_scope WHERE activity_id = #{activityId}")
    int purgeByActivityId(@Param("activityId") Long activityId);
}
