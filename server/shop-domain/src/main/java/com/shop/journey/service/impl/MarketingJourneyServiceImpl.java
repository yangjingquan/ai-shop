package com.shop.journey.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.coupon.service.CouponService;
import com.shop.customer.entity.CustomerMetricSnapshot;
import com.shop.customer.mapper.CustomerMetricSnapshotMapper;
import com.shop.customer.service.CustomerOperationsService;
import com.shop.journey.dto.MarketingJourneyRequest;
import com.shop.journey.dto.MarketingJourneyVO;
import com.shop.journey.entity.MarketingJourney;
import com.shop.journey.entity.MarketingJourneyEnrollment;
import com.shop.journey.entity.MarketingJourneyExecution;
import com.shop.journey.mapper.MarketingJourneyEnrollmentMapper;
import com.shop.journey.mapper.MarketingJourneyExecutionMapper;
import com.shop.journey.mapper.MarketingJourneyMapper;
import com.shop.journey.service.MarketingJourneyService;
import com.shop.notification.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingJourneyServiceImpl implements MarketingJourneyService {
    public static final Set<String> TRIGGERS = Set.of("REGISTER", "ORDER_PAID", "BROWSE_NO_PURCHASE", "COUPON_EXPIRING", "DORMANT", "MEMBER_UPGRADED");
    private static final String PENDING = "PENDING", SUCCESS = "SUCCESS", SKIPPED = "SKIPPED", CANCELLED = "CANCELLED";
    private final MarketingJourneyMapper journeyMapper;
    private final MarketingJourneyEnrollmentMapper enrollmentMapper;
    private final MarketingJourneyExecutionMapper executionMapper;
    private final CustomerMetricSnapshotMapper metricMapper;
    private final CustomerOperationsService customerOperationsService;
    private final CouponService couponService;
    private final UserNotificationService notificationService;

    @Override
    public List<MarketingJourneyVO> list(Long merchantId) {
        return journeyMapper.selectList(new LambdaQueryWrapper<MarketingJourney>().eq(MarketingJourney::getMerchantId, merchantId).orderByDesc(MarketingJourney::getUpdatedAt)).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public Long save(Long merchantId, Long id, MarketingJourneyRequest request) {
        validate(request);
        MarketingJourney journey = id == null ? null : owned(merchantId, id);
        if (journey == null) { journey = new MarketingJourney(); journey.setMerchantId(merchantId); }
        journey.setName(request.getName().trim()); journey.setTriggerType(request.getTriggerType().trim().toUpperCase()); journey.setSegmentId(request.getSegmentId());
        journey.setDelayMinutes(Math.max(0, request.getDelayMinutes())); journey.setCouponTemplateId(request.getCouponTemplateId());
        journey.setNotificationTitle(trim(request.getNotificationTitle(), 96)); journey.setNotificationContent(trim(request.getNotificationContent(), 500));
        journey.setFrequencyDays(Math.max(1, Math.min(90, request.getFrequencyDays()))); journey.setStopOnPaid(flag(request.getStopOnPaid())); journey.setStatus(flag(request.getStatus()));
        if (journey.getId() == null) journeyMapper.insert(journey); else journeyMapper.updateById(journey);
        if (journey.getStatus() == 0) enrollmentMapper.update(null, new LambdaUpdateWrapper<MarketingJourneyEnrollment>()
                .eq(MarketingJourneyEnrollment::getJourneyId, journey.getId())
                .eq(MarketingJourneyEnrollment::getStatus, PENDING)
                .set(MarketingJourneyEnrollment::getStatus, CANCELLED)
                .set(MarketingJourneyEnrollment::getSkipReason, "营销旅程已停用"));
        return journey.getId();
    }

    @Override
    @Transactional
    public void scan(Long merchantId, Long journeyId) {
        MarketingJourney journey = owned(merchantId, journeyId);
        if (journey.getStatus() != 1) return;
        customerOperationsService.refreshMerchantMetrics(merchantId);
        scanCurrentMetrics(journey);
    }

    private void scanCurrentMetrics(MarketingJourney journey) {
        Long merchantId = journey.getMerchantId();
        Set<Long> audience = new HashSet<>(customerOperationsService.segmentUserIds(merchantId, journey.getSegmentId()));
        metricMapper.selectList(new LambdaQueryWrapper<CustomerMetricSnapshot>().eq(CustomerMetricSnapshot::getMerchantId, merchantId)).forEach(metric -> {
            if (audience.contains(metric.getUserId()) && eligible(journey, metric)) enqueue(journey, metric);
        });
    }

    @Override
    @Transactional
    public void scanAll() {
        journeyMapper.selectList(new LambdaQueryWrapper<MarketingJourney>().eq(MarketingJourney::getStatus, 1)).stream()
                .collect(Collectors.groupingBy(MarketingJourney::getMerchantId)).forEach((merchantId, journeys) -> {
                    customerOperationsService.refreshMerchantMetrics(merchantId);
                    journeys.forEach(this::scanCurrentMetrics);
                });
    }

    @Override
    @Transactional
    public void processDue() {
        List<MarketingJourneyEnrollment> due = enrollmentMapper.selectList(new LambdaQueryWrapper<MarketingJourneyEnrollment>().eq(MarketingJourneyEnrollment::getStatus, PENDING).le(MarketingJourneyEnrollment::getExecuteAt, LocalDateTime.now()).orderByAsc(MarketingJourneyEnrollment::getExecuteAt).last("LIMIT 200"));
        for (MarketingJourneyEnrollment enrollment : due) {
            try { processEnrollment(enrollment); } catch (RuntimeException ex) { log.error("自动营销执行失败 enrollment={}", enrollment.getId(), ex); mark(enrollment, SKIPPED, "执行失败，请检查优惠券或消息配置"); }
        }
    }

    private void enqueue(MarketingJourney journey, CustomerMetricSnapshot metric) {
        LocalDateTime triggerAt = triggerAt(journey, metric); if (triggerAt == null) return;
        String triggerKey = triggerKey(journey, metric, triggerAt);
        if (enrollmentMapper.selectCount(new LambdaQueryWrapper<MarketingJourneyEnrollment>().eq(MarketingJourneyEnrollment::getJourneyId, journey.getId()).eq(MarketingJourneyEnrollment::getUserId, metric.getUserId()).eq(MarketingJourneyEnrollment::getTriggerKey, triggerKey)) > 0) return;
        LocalDateTime since = LocalDateTime.now().minusDays(journey.getFrequencyDays());
        if (executionMapper.selectCount(new LambdaQueryWrapper<MarketingJourneyExecution>().eq(MarketingJourneyExecution::getJourneyId, journey.getId()).eq(MarketingJourneyExecution::getUserId, metric.getUserId()).eq(MarketingJourneyExecution::getStatus, SUCCESS).ge(MarketingJourneyExecution::getExecutedAt, since)) > 0) return;
        MarketingJourneyEnrollment enrollment = new MarketingJourneyEnrollment(); enrollment.setMerchantId(journey.getMerchantId()); enrollment.setJourneyId(journey.getId()); enrollment.setUserId(metric.getUserId()); enrollment.setTriggerKey(triggerKey); enrollment.setTriggerAt(triggerAt); enrollment.setExecuteAt(triggerAt.plusMinutes(journey.getDelayMinutes())); enrollment.setStatus(PENDING); enrollment.setSkipReason("");
        try { enrollmentMapper.insert(enrollment); } catch (DuplicateKeyException ignored) { }
    }

    private void processEnrollment(MarketingJourneyEnrollment enrollment) {
        MarketingJourneyExecution completed = executionMapper.selectOne(new LambdaQueryWrapper<MarketingJourneyExecution>()
                .eq(MarketingJourneyExecution::getEnrollmentId, enrollment.getId()));
        if (completed != null) { mark(enrollment, SUCCESS, ""); return; }
        MarketingJourney journey = journeyMapper.selectById(enrollment.getJourneyId());
        if (journey == null || journey.getStatus() != 1 || !Objects.equals(journey.getMerchantId(), enrollment.getMerchantId())) { mark(enrollment, CANCELLED, "营销旅程已停用"); return; }
        CustomerMetricSnapshot metric = metricMapper.selectOne(new LambdaQueryWrapper<CustomerMetricSnapshot>().eq(CustomerMetricSnapshot::getMerchantId, enrollment.getMerchantId()).eq(CustomerMetricSnapshot::getUserId, enrollment.getUserId()));
        if (metric == null || !customerOperationsService.matchesSegment(enrollment.getMerchantId(), journey.getSegmentId(), enrollment.getUserId())) { mark(enrollment, SKIPPED, "当前用户不再属于目标人群"); return; }
        if (journey.getStopOnPaid() == 1 && !"ORDER_PAID".equals(journey.getTriggerType()) && metric.getLastPaidAt() != null && metric.getLastPaidAt().isAfter(enrollment.getTriggerAt())) { mark(enrollment, SKIPPED, "用户已完成支付"); return; }
        Long couponId = null, notificationId = null;
        if (journey.getCouponTemplateId() != null) couponId = couponService.issueTemplate(enrollment.getUserId(), enrollment.getMerchantId(), journey.getCouponTemplateId());
        if (!journey.getNotificationTitle().isBlank() || !journey.getNotificationContent().isBlank()) notificationId = notificationService.sendMarketing(enrollment.getUserId(), enrollment.getMerchantId(), journey.getNotificationTitle(), journey.getNotificationContent(), "/pages/coupon/list");
        MarketingJourneyExecution execution = new MarketingJourneyExecution(); execution.setMerchantId(enrollment.getMerchantId()); execution.setJourneyId(journey.getId()); execution.setEnrollmentId(enrollment.getId()); execution.setUserId(enrollment.getUserId()); execution.setCouponId(couponId); execution.setNotificationId(notificationId); execution.setStatus(SUCCESS); execution.setReason(""); execution.setExecutedAt(LocalDateTime.now()); executionMapper.insert(execution);
        mark(enrollment, SUCCESS, "");
    }

    private boolean eligible(MarketingJourney journey, CustomerMetricSnapshot metric) {
        LocalDateTime now = LocalDateTime.now();
        return switch (journey.getTriggerType()) {
            case "REGISTER" -> metric.getRegisteredAt() != null && !metric.getRegisteredAt().isBefore(journey.getCreatedAt()) && metric.getPaidOrderCount() == 0;
            case "ORDER_PAID" -> metric.getLastPaidAt() != null && !metric.getLastPaidAt().isBefore(journey.getCreatedAt());
            case "BROWSE_NO_PURCHASE" -> metric.getLastViewedAt() != null && !metric.getLastViewedAt().isBefore(journey.getCreatedAt()) && metric.getLastViewedAt().isAfter(now.minusDays(7)) && metric.getPaidOrderCount() == 0;
            case "COUPON_EXPIRING" -> metric.getExpiringCouponCount() > 0;
            case "DORMANT" -> metric.getLastPaidAt() != null && metric.getLastPaidAt().isBefore(now.minusDays(30));
            case "MEMBER_UPGRADED" -> metric.getMemberLevel() > 1 && metric.getMemberLevelUpdatedAt() != null && !metric.getMemberLevelUpdatedAt().isBefore(journey.getCreatedAt());
            default -> false;
        };
    }

    private LocalDateTime triggerAt(MarketingJourney journey, CustomerMetricSnapshot metric) {
        return switch (journey.getTriggerType()) {
            case "ORDER_PAID", "DORMANT" -> metric.getLastPaidAt();
            case "BROWSE_NO_PURCHASE" -> metric.getLastViewedAt();
            case "REGISTER" -> metric.getRegisteredAt();
            case "MEMBER_UPGRADED" -> metric.getMemberLevelUpdatedAt();
            default -> metric.getCalculatedAt() == null ? LocalDateTime.now() : metric.getCalculatedAt();
        };
    }

    private String triggerKey(MarketingJourney journey, CustomerMetricSnapshot metric, LocalDateTime triggerAt) {
        return journey.getTriggerType() + ":" + switch (journey.getTriggerType()) {
            case "ORDER_PAID", "BROWSE_NO_PURCHASE" -> triggerAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            case "MEMBER_UPGRADED" -> String.valueOf(metric.getMemberLevel());
            case "DORMANT", "COUPON_EXPIRING" -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            default -> String.valueOf(metric.getUserId());
        };
    }

    private void mark(MarketingJourneyEnrollment enrollment, String status, String reason) { enrollment.setStatus(status); enrollment.setSkipReason(reason); enrollmentMapper.updateById(enrollment); }
    private MarketingJourney owned(Long merchantId, Long id) { MarketingJourney journey = journeyMapper.selectById(id); if (journey == null || !Objects.equals(journey.getMerchantId(), merchantId)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "自动营销不存在"); return journey; }
    private void validate(MarketingJourneyRequest request) { String trigger = request.getTriggerType() == null ? "" : request.getTriggerType().trim().toUpperCase(); if (!TRIGGERS.contains(trigger)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不支持的触发条件"); if (request.getDelayMinutes() == null || request.getDelayMinutes() < 0 || request.getDelayMinutes() > 43200 || request.getFrequencyDays() == null || request.getFrequencyDays() < 1 || request.getFrequencyDays() > 90) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "延时或频控设置不合法"); if ((request.getCouponTemplateId() == null) && trim(request.getNotificationTitle(), 96).isBlank() && trim(request.getNotificationContent(), 500).isBlank()) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "至少配置发券或站内消息动作"); }
    private MarketingJourneyVO toVO(MarketingJourney source) { MarketingJourneyVO value = new MarketingJourneyVO(); value.setId(source.getId()); value.setName(source.getName()); value.setTriggerType(source.getTriggerType()); value.setSegmentId(source.getSegmentId()); value.setDelayMinutes(source.getDelayMinutes()); value.setCouponTemplateId(source.getCouponTemplateId()); value.setNotificationTitle(source.getNotificationTitle()); value.setNotificationContent(source.getNotificationContent()); value.setFrequencyDays(source.getFrequencyDays()); value.setStopOnPaid(source.getStopOnPaid()); value.setStatus(source.getStatus()); value.setUpdatedAt(source.getUpdatedAt()); List<MarketingJourneyExecution> executions = executionMapper.selectList(new LambdaQueryWrapper<MarketingJourneyExecution>().eq(MarketingJourneyExecution::getJourneyId, source.getId())); value.setEnrollmentCount(enrollmentMapper.selectCount(new LambdaQueryWrapper<MarketingJourneyEnrollment>().eq(MarketingJourneyEnrollment::getJourneyId, source.getId()))); value.setSuccessCount(executions.stream().filter(item -> SUCCESS.equals(item.getStatus())).count()); value.setSkippedCount(enrollmentMapper.selectCount(new LambdaQueryWrapper<MarketingJourneyEnrollment>().eq(MarketingJourneyEnrollment::getJourneyId, source.getId()).in(MarketingJourneyEnrollment::getStatus, List.of(SKIPPED, CANCELLED)))); value.setCouponCount(executions.stream().filter(item -> item.getCouponId() != null).count()); value.setNotificationCount(executions.stream().filter(item -> item.getNotificationId() != null).count()); return value; }
    private int flag(Integer value) { return Integer.valueOf(1).equals(value) ? 1 : 0; } private String trim(String value, int max) { if (value == null) return ""; String result = value.trim(); return result.length() > max ? result.substring(0, max) : result; }
}
