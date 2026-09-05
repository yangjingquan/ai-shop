package com.shop.bundle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.bundle.entity.BundleItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BundleItemMapper extends BaseMapper<BundleItem> {
    /** 套餐商品是整组替换配置，物理清理旧行，避免逻辑删除值参与唯一键造成冲突。 */
    @Delete("DELETE FROM bundle_item WHERE bundle_activity_id = #{activityId}")
    int purgeByActivityId(@Param("activityId") Long activityId);
}
