package com.shop.oplog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("op_log")
public class OpLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer operatorType;

    private Long operatorId;

    private String action;

    private String targetType;

    private String targetId;

    private String payload;

    private String ip;

    private LocalDateTime createdAt;
}
