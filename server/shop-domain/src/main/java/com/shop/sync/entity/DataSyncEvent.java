package com.shop.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("data_sync_event")
public class DataSyncEvent {
    @TableId(type = IdType.AUTO) private Long id;
    private String scope;
    private Long merchantId;
    private String resourceId;
    private String action;
    private LocalDateTime createdAt;
}
