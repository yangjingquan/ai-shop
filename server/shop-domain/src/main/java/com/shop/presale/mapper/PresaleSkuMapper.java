package com.shop.presale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.presale.entity.PresaleSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PresaleSkuMapper extends BaseMapper<PresaleSku> {
    @Select("SELECT * FROM presale_sku WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    PresaleSku selectForUpdate(@Param("id") Long id);

    @Update("UPDATE presale_sku SET deposit_count = deposit_count + #{quantity}, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int addDepositCount(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE presale_sku SET balance_sold_count = balance_sold_count + #{quantity}, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int addBalanceSoldCount(@Param("id") Long id, @Param("quantity") int quantity);
}
