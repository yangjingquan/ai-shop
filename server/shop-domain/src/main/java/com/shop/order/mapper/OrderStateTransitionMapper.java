package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.order.entity.OrderStateTransition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStateTransitionMapper extends BaseMapper<OrderStateTransition> {
}
