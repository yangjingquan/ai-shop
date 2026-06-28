package com.shop.oplog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.oplog.entity.OpLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpLogMapper extends BaseMapper<OpLog> {
}
