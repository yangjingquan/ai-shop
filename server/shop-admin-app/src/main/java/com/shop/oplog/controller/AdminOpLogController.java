package com.shop.oplog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.oplog.entity.OpLog;
import com.shop.oplog.mapper.OpLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/op-logs")
@RequiredArgsConstructor
public class AdminOpLogController {

    private final OpLogMapper opLogMapper;

    @GetMapping("/page")
    public ApiResult<PageResult<OpLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer operatorType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo) {
        LambdaQueryWrapper<OpLog> q = new LambdaQueryWrapper<OpLog>()
                .orderByDesc(OpLog::getId);
        if (operatorType != null) q.eq(OpLog::getOperatorType, operatorType);
        if (StringUtils.hasText(action)) q.like(OpLog::getAction, action.trim());
        if (StringUtils.hasText(targetType)) q.eq(OpLog::getTargetType, targetType.trim());
        if (createdFrom != null) q.ge(OpLog::getCreatedAt, createdFrom);
        if (createdTo != null) q.lt(OpLog::getCreatedAt, createdTo);
        IPage<OpLog> result = opLogMapper.selectPage(new Page<>(page, size), q);
        return ApiResult.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }
}
