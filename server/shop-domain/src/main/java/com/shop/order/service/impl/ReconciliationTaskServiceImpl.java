package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.order.dto.ReconciliationTaskVO;
import com.shop.order.entity.ReconciliationTask;
import com.shop.order.mapper.ReconciliationTaskMapper;
import com.shop.order.service.PaymentReconciliationService;
import com.shop.order.service.ReconciliationTaskService;
import com.shop.order.service.RefundReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReconciliationTaskServiceImpl implements ReconciliationTaskService {
    private final ReconciliationTaskMapper taskMapper;
    private final PaymentReconciliationService paymentReconciliationService;
    private final RefundReconciliationService refundReconciliationService;
    private final ObjectMapper objectMapper;

    @Override
    public ReconciliationTaskVO preview(String type, Long merchantId, LocalDateTime from, LocalDateTime to) {
        validate(type, from, to);
        ReconciliationTaskVO vo = new ReconciliationTaskVO();
        vo.setTaskType(normalize(type)); vo.setMerchantId(merchantId); vo.setRangeStart(from); vo.setRangeEnd(to);
        vo.setStatus("PREVIEW"); vo.setAffectedCount(100);
        return vo;
    }

    @Override
    @Transactional
    public ReconciliationTaskVO createAndRun(String type, Long merchantId, LocalDateTime from, LocalDateTime to, String requestedBy) {
        validate(type, from, to);
        String normalized = normalize(type);
        String key = normalized + ":" + (merchantId == null ? "all" : merchantId) + ":" + from + ":" + to;
        ReconciliationTask existing = taskMapper.selectOne(new LambdaQueryWrapper<ReconciliationTask>().eq(ReconciliationTask::getIdempotencyKey, key));
        if (existing != null) return toVo(existing);
        ReconciliationTask task = new ReconciliationTask();
        task.setTaskNo("RCT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
        task.setTaskType(normalized); task.setMerchantId(merchantId); task.setRangeStart(from); task.setRangeEnd(to);
        task.setStatus("RUNNING"); task.setIdempotencyKey(key); task.setRequestedBy(requestedBy == null ? "admin" : requestedBy);
        task.setStartedAt(LocalDateTime.now()); task.setErrorMessage("");
        try { taskMapper.insert(task); } catch (DuplicateKeyException ignored) {
            return toVo(taskMapper.selectOne(new LambdaQueryWrapper<ReconciliationTask>().eq(ReconciliationTask::getIdempotencyKey, key)));
        }
        try {
            int affected = "PAYMENT".equals(normalized) ? paymentReconciliationService.reconcilePending(100, merchantId, from, to) : refundReconciliationService.reconcilePending(100, merchantId, from, to);
            task.setSummaryJson(objectMapper.writeValueAsString(java.util.Map.of("affectedCount", affected, "scope", "pending reconciliation only")));
            task.setStatus("SUCCESS"); task.setFinishedAt(LocalDateTime.now()); taskMapper.updateById(task);
        } catch (Exception e) {
            task.setStatus("FAILED"); task.setErrorMessage(abbreviate(e.getMessage())); task.setFinishedAt(LocalDateTime.now()); taskMapper.updateById(task);
        }
        return toVo(task);
    }

    @Override
    public List<ReconciliationTaskVO> recent(String type, int limit) {
        return taskMapper.selectList(new LambdaQueryWrapper<ReconciliationTask>().eq(type != null && !type.isBlank(), ReconciliationTask::getTaskType, normalize(type)).orderByDesc(ReconciliationTask::getId).last("LIMIT " + Math.min(Math.max(limit, 1), 50))).stream().map(this::toVo).toList();
    }

    private void validate(String type, LocalDateTime from, LocalDateTime to) {
        String t = normalize(type); if (!"PAYMENT".equals(t) && !"REFUND".equals(t)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "仅支持支付或退款对账");
        if (from != null && to != null && !from.isBefore(to)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "对账结束时间必须晚于开始时间");
    }
    private String normalize(String type) { return type == null ? "" : type.trim().toUpperCase(Locale.ROOT); }
    private String abbreviate(String text) { return text == null ? "未知错误" : text.substring(0, Math.min(text.length(), 500)); }
    private ReconciliationTaskVO toVo(ReconciliationTask task) {
        ReconciliationTaskVO vo = new ReconciliationTaskVO(); vo.setTaskNo(task.getTaskNo()); vo.setTaskType(task.getTaskType()); vo.setMerchantId(task.getMerchantId()); vo.setRangeStart(task.getRangeStart()); vo.setRangeEnd(task.getRangeEnd()); vo.setStatus(task.getStatus()); vo.setErrorMessage(task.getErrorMessage()); vo.setStartedAt(task.getStartedAt()); vo.setFinishedAt(task.getFinishedAt()); vo.setCreatedAt(task.getCreatedAt());
        try { vo.setAffectedCount(task.getSummaryJson() == null ? 0 : objectMapper.readTree(task.getSummaryJson()).path("affectedCount").asInt()); } catch (Exception ignored) { vo.setAffectedCount(0); } return vo;
    }
}
