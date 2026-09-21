package com.shop.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.coupon.entity.UserCoupon;
import com.shop.coupon.enums.UserCouponStatus;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.customer.dto.CustomerOperationRequest;
import com.shop.customer.dto.CustomerOperationVO;
import com.shop.customer.entity.CustomerMetricSnapshot;
import com.shop.customer.entity.CustomerSegment;
import com.shop.customer.entity.CustomerSegmentMember;
import com.shop.customer.entity.CustomerTag;
import com.shop.customer.entity.CustomerTagBinding;
import com.shop.customer.mapper.CustomerMetricSnapshotMapper;
import com.shop.customer.mapper.CustomerSegmentMapper;
import com.shop.customer.mapper.CustomerSegmentMemberMapper;
import com.shop.customer.mapper.CustomerTagBindingMapper;
import com.shop.customer.mapper.CustomerTagMapper;
import com.shop.customer.service.CustomerOperationsService;
import com.shop.engagement.entity.UserProductFavorite;
import com.shop.engagement.entity.UserProductHistory;
import com.shop.engagement.mapper.UserProductFavoriteMapper;
import com.shop.engagement.mapper.UserProductHistoryMapper;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.points.entity.MemberProfile;
import com.shop.points.entity.PointsAccount;
import com.shop.points.mapper.MemberProfileMapper;
import com.shop.points.mapper.PointsAccountMapper;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOperationsServiceImpl implements CustomerOperationsService {
    private static final String SYSTEM = "SYSTEM";
    private static final String MANUAL = "MANUAL";
    private static final List<SystemTag> SYSTEM_TAGS = List.of(
            new SystemTag("NEW_CUSTOMER", "新客"), new SystemTag("FIRST_PURCHASE", "首购用户"),
            new SystemTag("REPEAT_PURCHASE", "复购用户"), new SystemTag("HIGH_VALUE", "高价值用户"),
            new SystemTag("DORMANT", "沉睡用户"), new SystemTag("COUPON_UNUSED", "持券未用"),
            new SystemTag("BROWSE_NO_PURCHASE", "浏览未购"), new SystemTag("FAVORITE_NO_PURCHASE", "收藏未购"));

    private final CustomerMetricSnapshotMapper metricMapper;
    private final CustomerTagMapper tagMapper;
    private final CustomerTagBindingMapper bindingMapper;
    private final CustomerSegmentMapper segmentMapper;
    private final CustomerSegmentMemberMapper segmentMemberMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final PointsAccountMapper pointsAccountMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserProductFavoriteMapper favoriteMapper;
    private final UserProductHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void refreshAllMetrics() {
        merchantMapper.selectList(new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, 1))
                .forEach(merchant -> refreshMerchantMetrics(merchant.getId()));
    }

    @Override
    @Transactional
    public void refreshMerchantMetrics(Long merchantId) {
        if (merchantId == null) return;
        ensureSystemTags(merchantId);
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getMerchantId, merchantId))
                .forEach(user -> refreshUserMetric(merchantId, user));
    }

    @Override
    @Transactional
    public List<CustomerOperationVO.Tag> tags(Long merchantId) {
        ensureSystemTags(merchantId);
        return tagMapper.selectList(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getMerchantId, merchantId)
                        .orderByAsc(CustomerTag::getTagType).orderByAsc(CustomerTag::getCreatedAt))
                .stream().map(tag -> tagVO(tag, countBoundUsers(merchantId, tag.getId()))).toList();
    }

    @Override
    @Transactional
    public Long saveTag(Long merchantId, Long tagId, CustomerOperationRequest.TagSave request) {
        CustomerTag tag = tagId == null ? null : ownedTag(merchantId, tagId);
        if (tag != null && SYSTEM.equals(tag.getTagType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统标签不可编辑");
        }
        String name = request.getName().trim();
        if (name.length() > 64) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "标签名称不能超过 64 个字符");
        if (tag == null) {
            tag = new CustomerTag();
            tag.setMerchantId(merchantId);
            tag.setCode("MANUAL_" + System.nanoTime());
            tag.setTagType(MANUAL);
        }
        tag.setName(name);
        tag.setColor(normalizeColor(request.getColor()));
        tag.setStatus(normalizeStatus(request.getStatus()));
        if (tag.getId() == null) tagMapper.insert(tag); else tagMapper.updateById(tag);
        return tag.getId();
    }

    @Override
    @Transactional
    public void bindTag(Long merchantId, Long tagId, CustomerOperationRequest.TagBinding request) {
        CustomerTag tag = ownedTag(merchantId, tagId);
        if (SYSTEM.equals(tag.getTagType())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统标签由指标自动维护");
        int status = normalizeStatus(request.getStatus());
        for (Long userId : request.getUserIds().stream().filter(Objects::nonNull).distinct().toList()) {
            assertMerchantUser(merchantId, userId);
            CustomerTagBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerTagBinding>()
                    .eq(CustomerTagBinding::getMerchantId, merchantId).eq(CustomerTagBinding::getUserId, userId)
                    .eq(CustomerTagBinding::getTagId, tagId));
            if (binding == null) {
                binding = new CustomerTagBinding();
                binding.setMerchantId(merchantId); binding.setUserId(userId); binding.setTagId(tagId);
                binding.setSource(MANUAL); binding.setStatus(status); bindingMapper.insert(binding);
            } else {
                binding.setStatus(status); binding.setSource(MANUAL); bindingMapper.updateById(binding);
            }
        }
    }

    @Override
    @Transactional
    public CustomerOperationVO.UserDetail userDetail(Long merchantId, Long userId) {
        User user = assertMerchantUser(merchantId, userId);
        CustomerMetricSnapshot metric = refreshUserMetric(merchantId, user);
        CustomerOperationVO.UserDetail detail = new CustomerOperationVO.UserDetail();
        fillUser(detail, user, metric, tagsForUsers(merchantId, List.of(userId)).getOrDefault(userId, List.of()));
        detail.setSystemTagNames(detail.getTags().stream().filter(tag -> SYSTEM.equals(tag.getTagType()))
                .map(CustomerOperationVO.Tag::getName).toList());
        detail.setRecentOrderAt(metric.getLastPaidAt());
        UserProductFavorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<UserProductFavorite>()
                .eq(UserProductFavorite::getMerchantId, merchantId).eq(UserProductFavorite::getUserId, userId)
                .orderByDesc(UserProductFavorite::getCreatedAt).last("LIMIT 1"));
        detail.setRecentFavoriteAt(favorite == null ? null : favorite.getCreatedAt());
        return detail;
    }

    @Override
    @Transactional
    public List<CustomerOperationVO.Segment> segments(Long merchantId) {
        return segmentMapper.selectList(new LambdaQueryWrapper<CustomerSegment>().eq(CustomerSegment::getMerchantId, merchantId)
                        .orderByDesc(CustomerSegment::getUpdatedAt)).stream().map(this::segmentVO).toList();
    }

    @Override
    @Transactional
    public Long saveSegment(Long merchantId, Long segmentId, CustomerOperationRequest.SegmentSave request) {
        validateSegmentRequest(merchantId, request);
        CustomerSegment segment = segmentId == null ? null : ownedSegment(merchantId, segmentId);
        if (segment == null) { segment = new CustomerSegment(); segment.setMerchantId(merchantId); }
        segment.setName(request.getName().trim());
        segment.setDescription(trim(request.getDescription(), 255));
        segment.setSegmentType(normalizeSegmentType(request.getSegmentType()));
        segment.setConditionJson(writeCondition(request.getCondition()));
        segment.setStatus(normalizeStatus(request.getStatus()));
        if (segment.getId() == null) segmentMapper.insert(segment); else segmentMapper.updateById(segment);
        rebuildSegmentInternal(merchantId, segment);
        return segment.getId();
    }

    @Override
    @Transactional
    public void rebuildSegment(Long merchantId, Long segmentId) {
        CustomerSegment segment = ownedSegment(merchantId, segmentId);
        rebuildSegmentInternal(merchantId, segment);
    }

    private void rebuildSegmentInternal(Long merchantId, CustomerSegment segment) {
        Long segmentId = segment.getId();
        refreshMerchantMetrics(merchantId);
        List<Long> userIds = matchedUserIds(merchantId, readCondition(segment.getConditionJson()));
        segment.setMemberCount(userIds.size());
        segment.setCalculatedAt(LocalDateTime.now());
        segmentMapper.updateById(segment);
        if ("STATIC".equals(segment.getSegmentType())) {
            segmentMemberMapper.delete(new LambdaQueryWrapper<CustomerSegmentMember>()
                    .eq(CustomerSegmentMember::getMerchantId, merchantId).eq(CustomerSegmentMember::getSegmentId, segmentId));
            LocalDateTime now = LocalDateTime.now();
            for (Long userId : userIds) {
                CustomerSegmentMember member = new CustomerSegmentMember();
                member.setMerchantId(merchantId); member.setSegmentId(segmentId); member.setUserId(userId); member.setSnapshotAt(now);
                segmentMemberMapper.insert(member);
            }
        }
    }

    @Override
    @Transactional
    public PageResult<CustomerOperationVO.UserSummary> segmentUsers(Long merchantId, Long segmentId, int page, int size) {
        CustomerSegment segment = ownedSegment(merchantId, segmentId);
        int safePage = Math.max(1, page), safeSize = Math.min(50, Math.max(1, size));
        List<Long> ids;
        if ("STATIC".equals(segment.getSegmentType())) {
            ids = segmentMemberMapper.selectList(new LambdaQueryWrapper<CustomerSegmentMember>()
                            .eq(CustomerSegmentMember::getMerchantId, merchantId).eq(CustomerSegmentMember::getSegmentId, segmentId)
                            .orderByDesc(CustomerSegmentMember::getSnapshotAt).orderByAsc(CustomerSegmentMember::getId))
                    .stream().map(CustomerSegmentMember::getUserId).toList();
        } else {
            refreshMerchantMetrics(merchantId);
            ids = matchedUserIds(merchantId, readCondition(segment.getConditionJson()));
            segment.setMemberCount(ids.size()); segment.setCalculatedAt(LocalDateTime.now()); segmentMapper.updateById(segment);
        }
        int from = Math.min((safePage - 1) * safeSize, ids.size());
        int to = Math.min(from + safeSize, ids.size());
        List<Long> pageIds = ids.subList(from, to);
        Map<Long, User> users = pageIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(pageIds).stream()
                .filter(user -> Objects.equals(merchantId, user.getMerchantId())).collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, CustomerMetricSnapshot> metrics = metricsForUsers(merchantId, pageIds);
        Map<Long, List<CustomerOperationVO.Tag>> tags = tagsForUsers(merchantId, pageIds);
        List<CustomerOperationVO.UserSummary> list = pageIds.stream().map(users::get).filter(Objects::nonNull).map(user -> {
            CustomerOperationVO.UserSummary value = new CustomerOperationVO.UserSummary();
            fillUser(value, user, metrics.get(user.getId()), tags.getOrDefault(user.getId(), List.of()));
            return value;
        }).toList();
        return PageResult.of(list, ids.size(), safePage, safeSize);
    }

    @Override
    @Transactional
    public List<Long> segmentUserIds(Long merchantId, Long segmentId) {
        if (segmentId == null) return metricMapper.selectList(new LambdaQueryWrapper<CustomerMetricSnapshot>()
                        .eq(CustomerMetricSnapshot::getMerchantId, merchantId))
                .stream().map(CustomerMetricSnapshot::getUserId).toList();
        CustomerSegment segment = ownedSegment(merchantId, segmentId);
        if ("STATIC".equals(segment.getSegmentType())) return segmentMemberMapper.selectList(new LambdaQueryWrapper<CustomerSegmentMember>()
                        .eq(CustomerSegmentMember::getMerchantId, merchantId).eq(CustomerSegmentMember::getSegmentId, segmentId))
                .stream().map(CustomerSegmentMember::getUserId).toList();
        return matchedUserIds(merchantId, readCondition(segment.getConditionJson()));
    }

    @Override
    @Transactional
    public boolean matchesSegment(Long merchantId, Long segmentId, Long userId) {
        return segmentUserIds(merchantId, segmentId).contains(userId);
    }

    private CustomerMetricSnapshot refreshUserMetric(Long merchantId, User user) {
        LocalDateTime now = LocalDateTime.now();
        List<Order> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId)
                        .eq(Order::getUserId, user.getId()).isNotNull(Order::getPayTime)
                        .notIn(Order::getStatus, List.of(OrderStatus.CANCELLED.getCode(), OrderStatus.GROUP_FAILED_WAIT_REFUND.getCode())));
        paidOrders.sort(Comparator.comparing(Order::getPayTime));
        MemberProfile profile = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getMerchantId, merchantId).eq(MemberProfile::getUserId, user.getId()));
        PointsAccount account = pointsAccountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>()
                .eq(PointsAccount::getMerchantId, merchantId).eq(PointsAccount::getUserId, user.getId()));
        long unused = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getMerchantId, merchantId)
                .eq(UserCoupon::getUserId, user.getId()).eq(UserCoupon::getStatus, UserCouponStatus.WAIT_USE.getCode())
                .and(query -> query.isNull(UserCoupon::getValidTo).or().gt(UserCoupon::getValidTo, now)));
        long expiring = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getMerchantId, merchantId)
                .eq(UserCoupon::getUserId, user.getId()).eq(UserCoupon::getStatus, UserCouponStatus.WAIT_USE.getCode())
                .between(UserCoupon::getValidTo, now, now.plusDays(3)));
        long favorites = favoriteMapper.selectCount(new LambdaQueryWrapper<UserProductFavorite>().eq(UserProductFavorite::getMerchantId, merchantId).eq(UserProductFavorite::getUserId, user.getId()));
        List<UserProductHistory> histories = historyMapper.selectList(new LambdaQueryWrapper<UserProductHistory>().eq(UserProductHistory::getMerchantId, merchantId).eq(UserProductHistory::getUserId, user.getId()));
        CustomerMetricSnapshot metric = metricMapper.selectOne(new LambdaQueryWrapper<CustomerMetricSnapshot>()
                .eq(CustomerMetricSnapshot::getMerchantId, merchantId).eq(CustomerMetricSnapshot::getUserId, user.getId()));
        if (metric == null) { metric = new CustomerMetricSnapshot(); metric.setMerchantId(merchantId); metric.setUserId(user.getId()); }
        metric.setPaidOrderCount(paidOrders.size());
        metric.setTotalPaidAmount(paidOrders.stream().map(Order::getPayAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        metric.setFirstPaidAt(paidOrders.isEmpty() ? null : paidOrders.get(0).getPayTime());
        metric.setLastPaidAt(paidOrders.isEmpty() ? null : paidOrders.get(paidOrders.size() - 1).getPayTime());
        metric.setMemberLevel(profile == null || profile.getLevel() == null ? 1 : profile.getLevel());
        metric.setPointsBalance(account == null || account.getBalance() == null ? 0 : account.getBalance());
        metric.setUnusedCouponCount((int) unused); metric.setExpiringCouponCount((int) expiring); metric.setFavoriteCount((int) favorites);
        metric.setHistoryCount(histories.size()); metric.setLastViewedAt(histories.stream().map(UserProductHistory::getLastViewedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
        metric.setRegisteredAt(user.getCreatedAt());
        metric.setMemberLevelUpdatedAt(profile == null ? null : profile.getUpdatedAt());
        metric.setCalculatedAt(now);
        if (metric.getId() == null) metricMapper.insert(metric); else metricMapper.updateById(metric);
        syncSystemTags(merchantId, user, metric);
        return metric;
    }

    private void ensureSystemTags(Long merchantId) {
        Map<String, CustomerTag> existing = tagMapper.selectList(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getMerchantId, merchantId))
                .stream().collect(Collectors.toMap(CustomerTag::getCode, tag -> tag));
        for (SystemTag spec : SYSTEM_TAGS) {
            if (existing.containsKey(spec.code())) continue;
            CustomerTag tag = new CustomerTag(); tag.setMerchantId(merchantId); tag.setCode(spec.code()); tag.setName(spec.name());
            tag.setTagType(SYSTEM); tag.setColor("#D86F22"); tag.setStatus(1); tagMapper.insert(tag);
        }
    }

    private void syncSystemTags(Long merchantId, User user, CustomerMetricSnapshot metric) {
        Map<String, CustomerTag> systemTags = tagMapper.selectList(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getMerchantId, merchantId).eq(CustomerTag::getTagType, SYSTEM))
                .stream().collect(Collectors.toMap(CustomerTag::getCode, tag -> tag));
        LocalDateTime now = LocalDateTime.now();
        Map<String, Boolean> matches = new LinkedHashMap<>();
        matches.put("NEW_CUSTOMER", metric.getPaidOrderCount() == 0 && user.getCreatedAt() != null && user.getCreatedAt().isAfter(now.minusDays(7)));
        matches.put("FIRST_PURCHASE", metric.getPaidOrderCount() == 1);
        matches.put("REPEAT_PURCHASE", metric.getPaidOrderCount() >= 2);
        matches.put("HIGH_VALUE", metric.getTotalPaidAmount().compareTo(new BigDecimal("1000")) >= 0);
        matches.put("DORMANT", metric.getLastPaidAt() != null && metric.getLastPaidAt().isBefore(now.minusDays(30)));
        matches.put("COUPON_UNUSED", metric.getUnusedCouponCount() > 0);
        matches.put("BROWSE_NO_PURCHASE", metric.getHistoryCount() > 0 && metric.getPaidOrderCount() == 0);
        matches.put("FAVORITE_NO_PURCHASE", metric.getFavoriteCount() > 0 && metric.getPaidOrderCount() == 0);
        for (Map.Entry<String, Boolean> entry : matches.entrySet()) {
            CustomerTag tag = systemTags.get(entry.getKey()); if (tag == null) continue;
            CustomerTagBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerTagBinding>().eq(CustomerTagBinding::getMerchantId, merchantId)
                    .eq(CustomerTagBinding::getUserId, user.getId()).eq(CustomerTagBinding::getTagId, tag.getId()));
            if (binding == null) { binding = new CustomerTagBinding(); binding.setMerchantId(merchantId); binding.setUserId(user.getId()); binding.setTagId(tag.getId()); binding.setSource(SYSTEM); binding.setStatus(entry.getValue() ? 1 : 0); bindingMapper.insert(binding); }
            else if (!Objects.equals(binding.getStatus(), entry.getValue() ? 1 : 0)) { binding.setStatus(entry.getValue() ? 1 : 0); binding.setSource(SYSTEM); bindingMapper.updateById(binding); }
        }
    }

    private List<Long> matchedUserIds(Long merchantId, CustomerOperationRequest.SegmentConditionGroup condition) {
        List<CustomerMetricSnapshot> metrics = metricMapper.selectList(new LambdaQueryWrapper<CustomerMetricSnapshot>().eq(CustomerMetricSnapshot::getMerchantId, merchantId));
        Map<Long, Set<Long>> tagIds = activeTagIdsForUsers(merchantId, metrics.stream().map(CustomerMetricSnapshot::getUserId).toList());
        boolean or = "OR".equalsIgnoreCase(condition.getLogic());
        return metrics.stream().filter(metric -> or
                        ? condition.getConditions().stream().anyMatch(item -> matches(metric, tagIds.getOrDefault(metric.getUserId(), Set.of()), item))
                        : condition.getConditions().stream().allMatch(item -> matches(metric, tagIds.getOrDefault(metric.getUserId(), Set.of()), item)))
                .map(CustomerMetricSnapshot::getUserId).toList();
    }

    private boolean matches(CustomerMetricSnapshot metric, Set<Long> tagIds, CustomerOperationRequest.SegmentCondition condition) {
        if ("tagId".equals(condition.getField())) return "HAS".equalsIgnoreCase(condition.getOperator()) && tagIds.contains(longValue(condition.getValue()));
        BigDecimal actual = switch (condition.getField()) {
            case "paidOrderCount" -> BigDecimal.valueOf(metric.getPaidOrderCount());
            case "totalPaidAmount" -> metric.getTotalPaidAmount();
            case "memberLevel" -> BigDecimal.valueOf(metric.getMemberLevel());
            case "pointsBalance" -> BigDecimal.valueOf(metric.getPointsBalance());
            case "unusedCouponCount" -> BigDecimal.valueOf(metric.getUnusedCouponCount());
            case "favoriteCount" -> BigDecimal.valueOf(metric.getFavoriteCount());
            case "historyCount" -> BigDecimal.valueOf(metric.getHistoryCount());
            default -> null;
        };
        if (actual == null) return false;
        int compare = actual.compareTo(new BigDecimal(condition.getValue()));
        return switch (condition.getOperator().toUpperCase()) { case "EQ" -> compare == 0; case "GTE" -> compare >= 0; case "LTE" -> compare <= 0; case "GT" -> compare > 0; case "LT" -> compare < 0; default -> false; };
    }

    private void validateSegmentRequest(Long merchantId, CustomerOperationRequest.SegmentSave request) {
        String type = normalizeSegmentType(request.getSegmentType());
        if (!List.of("DYNAMIC", "STATIC").contains(type) || request.getCondition().getConditions().size() > 8 || !List.of("AND", "OR").contains(request.getCondition().getLogic().toUpperCase())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "分群条件不合法");
        for (CustomerOperationRequest.SegmentCondition condition : request.getCondition().getConditions()) {
            if ("tagId".equals(condition.getField())) { if (!"HAS".equalsIgnoreCase(condition.getOperator())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "标签条件仅支持拥有标签"); ownedTag(merchantId, longValue(condition.getValue())); }
            else if (!List.of("paidOrderCount", "totalPaidAmount", "memberLevel", "pointsBalance", "unusedCouponCount", "favoriteCount", "historyCount").contains(condition.getField()) || !List.of("EQ", "GTE", "LTE", "GT", "LT").contains(condition.getOperator().toUpperCase())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不支持的分群条件");
            else { try { new BigDecimal(condition.getValue()); } catch (NumberFormatException ex) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "分群条件值必须为数字"); } }
        }
    }

    private Map<Long, CustomerMetricSnapshot> metricsForUsers(Long merchantId, Collection<Long> userIds) { if (userIds.isEmpty()) return Map.of(); return metricMapper.selectList(new LambdaQueryWrapper<CustomerMetricSnapshot>().eq(CustomerMetricSnapshot::getMerchantId, merchantId).in(CustomerMetricSnapshot::getUserId, userIds)).stream().collect(Collectors.toMap(CustomerMetricSnapshot::getUserId, metric -> metric)); }
    private Map<Long, List<CustomerOperationVO.Tag>> tagsForUsers(Long merchantId, Collection<Long> userIds) { if (userIds.isEmpty()) return Map.of(); Map<Long, CustomerTag> tags = tagMapper.selectList(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getMerchantId, merchantId)).stream().collect(Collectors.toMap(CustomerTag::getId, tag -> tag)); Map<Long, List<CustomerOperationVO.Tag>> result = new HashMap<>(); bindingMapper.selectList(new LambdaQueryWrapper<CustomerTagBinding>().eq(CustomerTagBinding::getMerchantId, merchantId).in(CustomerTagBinding::getUserId, userIds).eq(CustomerTagBinding::getStatus, 1)).forEach(binding -> { CustomerTag tag = tags.get(binding.getTagId()); if (tag != null && tag.getStatus() == 1) result.computeIfAbsent(binding.getUserId(), key -> new ArrayList<>()).add(tagVO(tag, null)); }); return result; }
    private Map<Long, Set<Long>> activeTagIdsForUsers(Long merchantId, Collection<Long> userIds) { if (userIds.isEmpty()) return Map.of(); return bindingMapper.selectList(new LambdaQueryWrapper<CustomerTagBinding>().eq(CustomerTagBinding::getMerchantId, merchantId).in(CustomerTagBinding::getUserId, userIds).eq(CustomerTagBinding::getStatus, 1)).stream().collect(Collectors.groupingBy(CustomerTagBinding::getUserId, Collectors.mapping(CustomerTagBinding::getTagId, Collectors.toSet()))); }
    private void fillUser(CustomerOperationVO.UserSummary target, User user, CustomerMetricSnapshot metric, List<CustomerOperationVO.Tag> tags) { target.setUserId(user.getId()); target.setNickname(user.getNickname()); target.setAvatar(user.getAvatar()); target.setPhone(user.getPhone()); target.setJoinedAt(user.getCreatedAt()); target.setLastLoginAt(user.getLastLoginAt()); target.setMetric(metricVO(metric)); target.setTags(tags); }
    private CustomerOperationVO.Metric metricVO(CustomerMetricSnapshot metric) { CustomerOperationVO.Metric value = new CustomerOperationVO.Metric(); if (metric == null) return value; value.setPaidOrderCount(metric.getPaidOrderCount()); value.setTotalPaidAmount(metric.getTotalPaidAmount()); value.setFirstPaidAt(metric.getFirstPaidAt()); value.setLastPaidAt(metric.getLastPaidAt()); value.setMemberLevel(metric.getMemberLevel()); value.setPointsBalance(metric.getPointsBalance()); value.setUnusedCouponCount(metric.getUnusedCouponCount()); value.setExpiringCouponCount(metric.getExpiringCouponCount()); value.setFavoriteCount(metric.getFavoriteCount()); value.setHistoryCount(metric.getHistoryCount()); value.setLastViewedAt(metric.getLastViewedAt()); value.setCalculatedAt(metric.getCalculatedAt()); return value; }
    private CustomerOperationVO.Tag tagVO(CustomerTag tag, Long count) { CustomerOperationVO.Tag value = new CustomerOperationVO.Tag(); value.setId(tag.getId()); value.setCode(tag.getCode()); value.setName(tag.getName()); value.setTagType(tag.getTagType()); value.setColor(tag.getColor()); value.setStatus(tag.getStatus()); value.setUserCount(count); return value; }
    private CustomerOperationVO.Segment segmentVO(CustomerSegment source) { CustomerOperationVO.Segment value = new CustomerOperationVO.Segment(); value.setId(source.getId()); value.setName(source.getName()); value.setDescription(source.getDescription()); value.setSegmentType(source.getSegmentType()); value.setCondition(readCondition(source.getConditionJson())); value.setMemberCount(source.getMemberCount()); value.setCalculatedAt(source.getCalculatedAt()); value.setStatus(source.getStatus()); value.setUpdatedAt(source.getUpdatedAt()); return value; }
    private CustomerTag ownedTag(Long merchantId, Long id) { CustomerTag tag = tagMapper.selectById(id); if (tag == null || !Objects.equals(tag.getMerchantId(), merchantId)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户标签不存在"); return tag; }
    private CustomerSegment ownedSegment(Long merchantId, Long id) { CustomerSegment segment = segmentMapper.selectById(id); if (segment == null || !Objects.equals(segment.getMerchantId(), merchantId)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户分群不存在"); return segment; }
    private User assertMerchantUser(Long merchantId, Long userId) { User user = userMapper.selectById(userId); if (user == null || !Objects.equals(user.getMerchantId(), merchantId)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户不存在或不属于当前商户"); return user; }
    private long countBoundUsers(Long merchantId, Long tagId) { return bindingMapper.selectCount(new LambdaQueryWrapper<CustomerTagBinding>().eq(CustomerTagBinding::getMerchantId, merchantId).eq(CustomerTagBinding::getTagId, tagId).eq(CustomerTagBinding::getStatus, 1)); }
    private String writeCondition(CustomerOperationRequest.SegmentConditionGroup value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "分群条件保存失败"); } }
    private CustomerOperationRequest.SegmentConditionGroup readCondition(String value) { try { return objectMapper.readValue(value, new TypeReference<>() {}); } catch (Exception ex) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "分群条件解析失败"); } }
    private int normalizeStatus(Integer value) { return Integer.valueOf(1).equals(value) ? 1 : 0; }
    private String normalizeColor(String value) { return value != null && value.matches("#[0-9a-fA-F]{6}") ? value : "#D86F22"; }
    private String normalizeSegmentType(String value) { return value == null ? "" : value.trim().toUpperCase(); }
    private String trim(String value, int max) { if (value == null) return ""; String result = value.trim(); return result.length() > max ? result.substring(0, max) : result; }
    private long longValue(String value) { try { return Long.parseLong(value); } catch (NumberFormatException ex) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "标签值不合法"); } }
    private record SystemTag(String code, String name) {}
}
