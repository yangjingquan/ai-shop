package com.shop.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("<script>SELECT * FROM cart_item WHERE user_id = #{userId} AND deleted = 0 "
            + "AND id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "FOR UPDATE</script>")
    List<CartItem> selectOwnedForUpdate(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
