package com.shop.groupbuy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.groupbuy.entity.GroupBuyGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupBuyGroupMapper extends BaseMapper<GroupBuyGroup> {
    @Select("SELECT * FROM group_buy_group WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    GroupBuyGroup selectByIdForUpdate(@Param("id") Long id);
}
