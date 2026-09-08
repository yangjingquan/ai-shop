package com.shop.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.sync.entity.DataSyncEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSyncEventMapper extends BaseMapper<DataSyncEvent> { }
