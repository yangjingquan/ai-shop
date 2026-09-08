package com.shop.lottery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.service.CouponService;
import com.shop.lottery.dto.*;
import com.shop.lottery.entity.*;
import com.shop.lottery.enums.LotteryActivityStatus;
import com.shop.lottery.enums.LotteryPrizeType;
import com.shop.lottery.mapper.*;
import com.shop.lottery.service.LotteryService;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.dto.AddressSnapshot;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.OrderType;
import com.shop.order.enums.FulfillmentMethod;
import com.shop.order.mapper.OrderItemMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderDomainModel;
import com.shop.order.service.OrderStateMachine;
import com.shop.points.entity.MemberProfile;
import com.shop.points.entity.PointsAccount;
import com.shop.points.mapper.MemberProfileMapper;
import com.shop.points.mapper.PointsAccountMapper;
import com.shop.points.service.PointsMemberService;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LotteryServiceImpl implements LotteryService {
    private static final DateTimeFormatter DRAW_NO_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LotteryActivityMapper activityMapper;
    private final LotteryPrizeMapper prizeMapper;
    private final LotteryDrawRecordMapper drawMapper;
    private final LotteryRewardMapper rewardMapper;
    private final MarketingFeatureService featureService;
    private final PointsMemberService pointsService;
    private final CouponService couponService;
    private final CouponTemplateMapper couponTemplateMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final PointsAccountMapper pointsAccountMapper;
    private final UserAddressMapper addressMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ObjectMapper objectMapper;
    private final OrderStateMachine orderStateMachine;

    @Override
    public LotteryActivityVO current(Long merchantId, Long userId) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.LOTTERY_BLIND_BOX);
        LocalDateTime now = LocalDateTime.now();
        LotteryActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<LotteryActivity>()
                .eq(LotteryActivity::getMerchantId, merchantId).eq(LotteryActivity::getStatus, 1)
                .le(LotteryActivity::getStartAt, now).ge(LotteryActivity::getEndAt, now)
                .orderByDesc(LotteryActivity::getId).last("LIMIT 1"));
        return activity == null ? null : toActivityVO(activity, userId, true);
    }

    @Override
    public LotteryActivityVO getActive(Long merchantId, Long userId, Long activityId) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.LOTTERY_BLIND_BOX);
        LotteryActivity activity = ownedActivity(merchantId, activityId);
        if (!isOnline(activity, LocalDateTime.now())) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "活动已结束或暂未开启");
        return toActivityVO(activity, userId, true);
    }

    @Override
    @Transactional
    public LotteryDrawVO draw(Long merchantId, Long userId, Long activityId, LotteryDrawRequest request) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.LOTTERY_BLIND_BOX);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        LotteryActivity activity = activityMapper.selectOne(new LambdaQueryWrapper<LotteryActivity>()
                .eq(LotteryActivity::getId, activityId).eq(LotteryActivity::getMerchantId, merchantId)
                .last("FOR UPDATE"));
        if (activity == null) throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND);
        if (!isOnline(activity, LocalDateTime.now())) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "活动已结束或暂未开启");
        String key = request.getIdempotencyKey().trim();
        if (key.length() > 96) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "请求标识过长");
        LotteryDrawRecord existing = drawMapper.selectOne(new LambdaQueryWrapper<LotteryDrawRecord>()
                .eq(LotteryDrawRecord::getActivityId, activityId).eq(LotteryDrawRecord::getUserId, userId)
                .eq(LotteryDrawRecord::getIdempotencyKey, key));
        if (existing != null) return toDrawVO(existing);
        assertEligible(activity, userId);
        LocalDate today = LocalDate.now();
        long used = drawMapper.selectCount(new LambdaQueryWrapper<LotteryDrawRecord>()
                .eq(LotteryDrawRecord::getActivityId, activityId).eq(LotteryDrawRecord::getUserId, userId)
                .eq(LotteryDrawRecord::getDrawDate, today));
        if (used >= Math.max(1, activity.getDailyChances())) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "今日抽奖次数已用完");
        List<LotteryPrize> available = prizeMapper.selectList(new LambdaQueryWrapper<LotteryPrize>()
                .eq(LotteryPrize::getActivityId, activityId).eq(LotteryPrize::getMerchantId, merchantId).eq(LotteryPrize::getStatus, 1)
                .gt(LotteryPrize::getProbability, BigDecimal.ZERO)
                .and(q -> q.eq(LotteryPrize::getTotalStock, 0).or().gt(LotteryPrize::getRemainingStock, 0))
                .orderByAsc(LotteryPrize::getSort).orderByAsc(LotteryPrize::getId));
        LotteryPrize prize = pick(available);
        if (prize == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "奖品已全部领完");
        if (prize.getTotalStock() != null && prize.getTotalStock() > 0 && prizeMapper.deductStock(prize.getId()) == 0) {
            available.remove(prize); prize = pick(available);
            if (prize == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "奖品库存不足，请稍后再试");
            if (prize.getTotalStock() != null && prize.getTotalStock() > 0 && prizeMapper.deductStock(prize.getId()) == 0) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "奖品库存不足，请稍后再试");
        }
        LotteryDrawRecord record = new LotteryDrawRecord();
        record.setDrawNo("LT" + LocalDateTime.now().format(DRAW_NO_TIME) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        record.setActivityId(activityId); record.setMerchantId(merchantId); record.setUserId(userId); record.setDrawDate(today);
        record.setPrizeId(prize.getId()); record.setPrizeName(prize.getName()); record.setPrizeType(prize.getPrizeType());
        record.setProbabilitySnapshot(prize.getProbability()); record.setProbabilityVersion(activity.getProbabilityVersion()); record.setIdempotencyKey(key);
        drawMapper.insert(record);
        LotteryReward reward = createReward(record, activity, prize);
        return toDrawVO(record, reward);
    }

    @Override
    public List<LotteryDrawVO> records(Long merchantId, Long userId, Long activityId) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.LOTTERY_BLIND_BOX); ownedActivity(merchantId, activityId);
        return drawMapper.selectList(new LambdaQueryWrapper<LotteryDrawRecord>().eq(LotteryDrawRecord::getMerchantId, merchantId)
                        .eq(LotteryDrawRecord::getUserId, userId).eq(LotteryDrawRecord::getActivityId, activityId)
                        .orderByDesc(LotteryDrawRecord::getId).last("LIMIT 50"))
                .stream().map(this::toDrawVO).toList();
    }

    @Override
    public List<LotteryRewardVO> userRewards(Long merchantId, Long userId) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.LOTTERY_BLIND_BOX);
        return rewardMapper.selectList(new LambdaQueryWrapper<LotteryReward>().eq(LotteryReward::getMerchantId, merchantId)
                        .eq(LotteryReward::getUserId, userId).orderByDesc(LotteryReward::getId).last("LIMIT 100"))
                .stream().map(this::toRewardVO).toList();
    }

    @Override
    @Transactional
    public void saveAddress(Long merchantId, Long userId, Long rewardId, Long addressId) {
        featureService.assertEnabled(merchantId, MarketingActivityCode.LOTTERY_BLIND_BOX);
        LotteryReward reward = rewardMapper.selectOne(new LambdaQueryWrapper<LotteryReward>().eq(LotteryReward::getId, rewardId)
                .eq(LotteryReward::getMerchantId, merchantId).eq(LotteryReward::getUserId, userId).last("FOR UPDATE"));
        if (reward == null || !"PHYSICAL".equals(reward.getPrizeType()) || reward.getStatus() != 2) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "该奖品当前不可填写地址");
        LotteryPrize prize = prizeMapper.selectById(reward.getPrizeId());
        UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getId, addressId).eq(UserAddress::getUserId, userId));
        Product product = prize == null ? null : productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getId, prize.getProductId()).eq(Product::getMerchantId, merchantId));
        ProductSku sku = prize == null ? null : skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getId, prize.getSkuId()).eq(ProductSku::getProductId, prize.getProductId()));
        if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        if (product == null || sku == null || !Integer.valueOf(1).equals(product.getStatus()) || !Integer.valueOf(1).equals(product.getAuditStatus())) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        if (skuMapper.deductStock(sku.getId(), 1) == 0) throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        Order order = new Order(); String orderNo = "LR" + LocalDateTime.now().format(DRAW_NO_TIME) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        order.setOrderNo(orderNo); order.setUserId(userId); order.setMerchantId(merchantId); order.setStatus(OrderStatus.WAIT_SHIP.getCode()); OrderDomainModel.initialize(order, OrderType.LOTTERY_PHYSICAL, FulfillmentMethod.EXPRESS); order.setLotteryRewardId(reward.getId());
        order.setTotalAmount(BigDecimal.ZERO); order.setFreightAmount(BigDecimal.ZERO); order.setDiscountAmount(BigDecimal.ZERO); order.setPayAmount(BigDecimal.ZERO); order.setPayMethod(3); order.setPayTime(LocalDateTime.now()); order.setRemark("抽奖实物奖品，商家包邮");
        try { order.setAddressSnapshot(objectMapper.writeValueAsString(new AddressSnapshot(address.getReceiver(), address.getPhone(), address.getRegion(), address.getDetail()))); } catch (Exception ex) { throw new IllegalStateException("地址快照失败", ex); }
        OrderDomainModel.refreshOrderSnapshot(order); orderMapper.insert(order); orderStateMachine.recordCreated(order, "LOTTERY_PHYSICAL_ORDER_CREATED");
        OrderItem item = new OrderItem(); item.setOrderId(order.getId()); item.setOrderNo(orderNo); item.setProductId(product.getId()); item.setSkuId(sku.getId()); item.setProductName(product.getName()); item.setMainImage(product.getMainImage()); item.setSpecText(sku.getSpecText()); item.setUnitPrice(BigDecimal.ZERO); item.setQuantity(1); item.setSubtotal(BigDecimal.ZERO); OrderDomainModel.refreshItemSnapshot(item, order); orderItemMapper.insert(item);
        reward.setOrderNo(orderNo); reward.setAddressSnapshot(order.getAddressSnapshot()); reward.setStatus(3); reward.setClaimedAt(LocalDateTime.now()); rewardMapper.updateById(reward);
    }

    @Override public List<LotteryActivityVO> merchantList(Long merchantId) { return activityMapper.selectList(new LambdaQueryWrapper<LotteryActivity>().eq(LotteryActivity::getMerchantId, merchantId).orderByDesc(LotteryActivity::getId)).stream().map(a -> toActivityVO(a, null, true)).toList(); }
    @Override public LotteryActivityVO merchantGet(Long merchantId, Long activityId) { return toActivityVO(ownedActivity(merchantId, activityId), null, true); }

    @Override @Transactional public Long create(Long merchantId, LotteryActivitySaveRequest request) { validateRequest(merchantId, request, null); LotteryActivity activity = new LotteryActivity(); apply(activity, merchantId, request); activityMapper.insert(activity); savePrizes(merchantId, activity.getId(), request.getPrizes()); return activity.getId(); }
    @Override @Transactional public void update(Long merchantId, Long activityId, LotteryActivitySaveRequest request) { LotteryActivity activity = ownedActivity(merchantId, activityId); validateRequest(merchantId, request, activityId); apply(activity, merchantId, request); activity.setProbabilityVersion(Math.max(1, Optional.ofNullable(activity.getProbabilityVersion()).orElse(1)) + 1); activityMapper.updateById(activity); prizeMapper.delete(new LambdaQueryWrapper<LotteryPrize>().eq(LotteryPrize::getActivityId, activityId)); savePrizes(merchantId, activityId, request.getPrizes()); }
    @Override @Transactional public void updateStatus(Long merchantId, Long activityId, Integer status) { LotteryActivity activity = ownedActivity(merchantId, activityId); if (status == null || Arrays.stream(LotteryActivityStatus.values()).noneMatch(s -> s.getCode() == status)) throw new BusinessException(ErrorCode.PARAM_ERROR); if (status == 1) validateActiveWindow(merchantId, activityId, activity.getStartAt(), activity.getEndAt()); activity.setStatus(status); activityMapper.updateById(activity); }

    @Override public LotteryStatsVO stats(Long merchantId, Long activityId) { ownedActivity(merchantId, activityId); List<LotteryDrawRecord> draws = drawMapper.selectList(new LambdaQueryWrapper<LotteryDrawRecord>().eq(LotteryDrawRecord::getMerchantId, merchantId).eq(LotteryDrawRecord::getActivityId, activityId)); List<LotteryReward> rewards = rewardMapper.selectList(new LambdaQueryWrapper<LotteryReward>().eq(LotteryReward::getMerchantId, merchantId).eq(LotteryReward::getActivityId, activityId)); LotteryStatsVO vo = new LotteryStatsVO(); vo.setDrawCount(draws.size()); vo.setParticipants(draws.stream().map(LotteryDrawRecord::getUserId).distinct().count()); vo.setRewardsIssued(rewards.stream().filter(r -> r.getStatus() != null && r.getStatus() > 0 && r.getStatus() < 6).count()); vo.setRewardsPending(rewards.stream().filter(r -> r.getStatus() != null && (r.getStatus() == 2 || r.getStatus() == 3)).count()); vo.setCouponRewards(rewards.stream().filter(r -> "COUPON".equals(r.getPrizeType())).count()); vo.setPhysicalRewards(rewards.stream().filter(r -> "PHYSICAL".equals(r.getPrizeType())).count()); vo.setPointsRewards(rewards.stream().filter(r -> "POINTS".equals(r.getPrizeType())).count()); vo.setPointsCost(BigDecimal.valueOf(rewards.stream().filter(r -> r.getPointsAmount() != null).mapToInt(r -> r.getPointsAmount()).sum())); return vo; }
    @Override public List<LotteryRewardVO> merchantRewards(Long merchantId, Long activityId) { ownedActivity(merchantId, activityId); return rewardMapper.selectList(new LambdaQueryWrapper<LotteryReward>().eq(LotteryReward::getMerchantId, merchantId).eq(LotteryReward::getActivityId, activityId).orderByDesc(LotteryReward::getId)).stream().map(this::toRewardVO).toList(); }
    @Override @Transactional public void updateRewardStatus(Long merchantId, Long activityId, Long rewardId, Integer status) { LotteryReward reward = rewardMapper.selectOne(new LambdaQueryWrapper<LotteryReward>().eq(LotteryReward::getId, rewardId).eq(LotteryReward::getMerchantId, merchantId).eq(LotteryReward::getActivityId, activityId)); if (reward == null) throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND); if (status == null || status < 0 || status > 5) throw new BusinessException(ErrorCode.PARAM_ERROR); reward.setStatus(status); rewardMapper.updateById(reward); }

    private void validateRequest(Long merchantId, LotteryActivitySaveRequest request, Long activityId) { if (request.getDailyChances() == null || request.getDailyChances() < 1 || request.getStartAt() == null || request.getEndAt() == null || !request.getEndAt().isAfter(request.getStartAt())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "活动时间或每日次数不合法"); LotteryCondition c = request.getCondition() == null ? new LotteryCondition() : request.getCondition(); if (Boolean.TRUE.equals(c.getFirstOrderOnly()) && Boolean.TRUE.equals(c.getRepurchaseOnly())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "首单和复购条件不能同时开启"); if (request.getPrizes() == null || request.getPrizes().isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "至少配置一个奖品"); BigDecimal sum = request.getPrizes().stream().filter(p -> p.getStatus() == null || p.getStatus() == 1).map(LotteryPrizeRequest::getProbability).reduce(BigDecimal.ZERO, BigDecimal::add); if (sum.subtract(ONE).abs().compareTo(new BigDecimal("0.0001")) > 0) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "奖品概率总和必须为100%"); if (request.getStatus() != null && request.getStatus() == 1) validateActiveWindow(merchantId, activityId, request.getStartAt(), request.getEndAt()); request.getPrizes().forEach(p -> { if (!LotteryPrizeType.valid(p.getPrizeType())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "奖品类型不合法"); if ("POINTS".equals(p.getPrizeType()) && (p.getPointsAmount() == null || p.getPointsAmount() <= 0)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "积分奖品需要配置积分数量"); if ("COUPON".equals(p.getPrizeType()) && p.getCouponTemplateId() == null) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "优惠券奖品需要选择优惠券模板"); if ("PHYSICAL".equals(p.getPrizeType()) && (p.getProductId() == null || p.getSkuId() == null)) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "实物奖品需要配置商品和SKU"); validateRefs(merchantId, p); }); }
    private void validateRefs(Long merchantId, LotteryPrizeRequest p) { if (p.getCouponTemplateId() != null && couponTemplateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>().eq(CouponTemplate::getId, p.getCouponTemplateId()).eq(CouponTemplate::getMerchantId, merchantId)) == null) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "优惠券模板不属于当前商家"); if (p.getProductId() != null) { Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getId, p.getProductId()).eq(Product::getMerchantId, merchantId)); ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getId, p.getSkuId()).eq(ProductSku::getProductId, p.getProductId())); if (product == null || sku == null) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "实物商品或SKU不存在"); } }
    private void validateActiveWindow(Long merchantId, Long activityId, LocalDateTime start, LocalDateTime end) { long conflict = activityMapper.selectCount(new LambdaQueryWrapper<LotteryActivity>().eq(LotteryActivity::getMerchantId, merchantId).eq(LotteryActivity::getStatus, 1).ne(activityId != null, LotteryActivity::getId, activityId).lt(LotteryActivity::getStartAt, end).gt(LotteryActivity::getEndAt, start)); if (conflict > 0) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "已有活动在线或排期重叠"); }
    private void apply(LotteryActivity a, Long merchantId, LotteryActivitySaveRequest r) { a.setMerchantId(merchantId); a.setName(r.getName().trim()); a.setThemeImage(Optional.ofNullable(r.getThemeImage()).orElse("")); a.setEntryImage(Optional.ofNullable(r.getEntryImage()).orElse("")); a.setRuleText(r.getRuleText().trim()); try { a.setConditionJson(objectMapper.writeValueAsString(r.getCondition() == null ? new LotteryCondition() : r.getCondition())); } catch (Exception e) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "参与条件配置不合法"); } a.setDailyChances(r.getDailyChances()); a.setStartAt(r.getStartAt()); a.setEndAt(r.getEndAt()); a.setStatus(r.getStatus() == null ? 0 : r.getStatus()); if (a.getProbabilityVersion() == null) a.setProbabilityVersion(1); }
    private void savePrizes(Long merchantId, Long activityId, List<LotteryPrizeRequest> requests) { for (LotteryPrizeRequest r : requests) { LotteryPrize p = new LotteryPrize(); p.setActivityId(activityId); p.setMerchantId(merchantId); p.setName(r.getName().trim()); p.setPrizeType(r.getPrizeType()); p.setImage(Optional.ofNullable(r.getImage()).orElse("")); p.setPointsAmount(r.getPointsAmount()); p.setCouponTemplateId(r.getCouponTemplateId()); p.setProductId(r.getProductId()); p.setSkuId(r.getSkuId()); p.setTotalStock(Optional.ofNullable(r.getTotalStock()).orElse(0)); p.setRemainingStock(p.getTotalStock()); p.setProbability(r.getProbability()); p.setValidityDays(Optional.ofNullable(r.getValidityDays()).orElse(0)); p.setSort(Optional.ofNullable(r.getSort()).orElse(0)); p.setStatus(Optional.ofNullable(r.getStatus()).orElse(1)); prizeMapper.insert(p); } }
    private LotteryActivity ownedActivity(Long merchantId, Long id) { LotteryActivity a = activityMapper.selectOne(new LambdaQueryWrapper<LotteryActivity>().eq(LotteryActivity::getId, id).eq(LotteryActivity::getMerchantId, merchantId)); if (a == null) throw new BusinessException(ErrorCode.MARKETING_ACTIVITY_NOT_FOUND); return a; }
    private boolean isOnline(LotteryActivity a, LocalDateTime now) { return a != null && a.getStatus() == 1 && !now.isBefore(a.getStartAt()) && now.isBefore(a.getEndAt()); }
    private LotteryPrize pick(List<LotteryPrize> prizes) { if (prizes == null || prizes.isEmpty()) return null; BigDecimal total = prizes.stream().map(LotteryPrize::getProbability).reduce(BigDecimal.ZERO, BigDecimal::add); double value = RANDOM.nextDouble() * total.doubleValue(); double cursor = 0; for (LotteryPrize p : prizes) { cursor += p.getProbability().doubleValue(); if (value <= cursor) return p; } return prizes.get(prizes.size() - 1); }
    private void assertEligible(LotteryActivity a, Long userId) { LotteryCondition c = condition(a); MemberProfile m = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId).eq(MemberProfile::getMerchantId, a.getMerchantId())); int level = m == null || m.getLevel() == null ? 1 : m.getLevel(); if (level < Math.max(1, Optional.ofNullable(c.getMinMemberLevel()).orElse(1))) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "会员等级未达到参与条件"); PointsAccount account = pointsAccountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>().eq(PointsAccount::getUserId, userId).eq(PointsAccount::getMerchantId, a.getMerchantId())); if ((account == null ? 0 : Optional.ofNullable(account.getBalance()).orElse(0)) < Math.max(0, Optional.ofNullable(c.getMinPoints()).orElse(0))) throw new BusinessException(ErrorCode.POINTS_NOT_ENOUGH); long paid = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId).eq(Order::getMerchantId, a.getMerchantId()).in(Order::getStatus, List.of(OrderStatus.WAIT_SHIP.getCode(), OrderStatus.WAIT_RECEIVE.getCode(), OrderStatus.FINISHED.getCode(), OrderStatus.WAIT_GROUP.getCode(), OrderStatus.GROUP_SUCCESS.getCode())).isNotNull(Order::getPayTime)); if (Boolean.TRUE.equals(c.getFirstOrderOnly()) && paid > 0) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前活动仅限首单用户参与"); if (Boolean.TRUE.equals(c.getRepurchaseOnly()) && paid == 0) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前活动仅限复购用户参与"); }
    private LotteryCondition condition(LotteryActivity a) { try { return objectMapper.readValue(Optional.ofNullable(a.getConditionJson()).orElse("{}"), LotteryCondition.class); } catch (Exception e) { return new LotteryCondition(); } }
    private LotteryReward createReward(LotteryDrawRecord record, LotteryActivity a, LotteryPrize p) { LotteryReward r = new LotteryReward(); r.setDrawRecordId(record.getId()); r.setActivityId(a.getId()); r.setMerchantId(a.getMerchantId()); r.setUserId(record.getUserId()); r.setPrizeId(p.getId()); r.setPrizeName(p.getName()); r.setPrizeType(p.getPrizeType()); r.setPointsAmount(p.getPointsAmount()); r.setCouponTemplateId(p.getCouponTemplateId()); r.setStatus(0); try { if ("PHYSICAL".equals(p.getPrizeType())) r.setStatus(2); else if (p.getPointsAmount() != null && p.getPointsAmount() > 0) { pointsService.rewardLotteryPoints(record.getUserId(), a.getMerchantId(), p.getPointsAmount(), record.getDrawNo(), p.getName(), Optional.ofNullable(p.getValidityDays()).orElse(0)); r.setStatus(1); r.setIssuedAt(LocalDateTime.now()); } else if (p.getCouponTemplateId() != null) { r.setCouponId(couponService.issueTemplate(record.getUserId(), a.getMerchantId(), p.getCouponTemplateId())); r.setStatus(1); r.setIssuedAt(LocalDateTime.now()); } else { r.setStatus(1); r.setIssuedAt(LocalDateTime.now()); } } catch (Exception e) { r.setStatus(6); r.setFailureReason(e.getMessage()); } rewardMapper.insert(r); return r; }
    private LotteryActivityVO toActivityVO(LotteryActivity a, Long userId, boolean includePrizes) {
        LotteryActivityVO v = new LotteryActivityVO();
        v.setId(a.getId()); v.setName(a.getName()); v.setThemeImage(a.getThemeImage()); v.setEntryImage(a.getEntryImage());
        v.setRuleText(a.getRuleText()); v.setCondition(condition(a)); v.setDailyChances(a.getDailyChances());
        v.setStartAt(a.getStartAt()); v.setEndAt(a.getEndAt()); v.setStatus(a.getStatus());
        v.setProbabilityVersion(a.getProbabilityVersion()); v.setActive(isOnline(a, LocalDateTime.now()));
        String statusText = v.getActive() ? "进行中" : a.getStatus() == 0 ? "草稿" : a.getStatus() == 2 ? "已暂停" : a.getEndAt().isBefore(LocalDateTime.now()) ? "已结束" : "未开始";
        v.setStatusText(statusText);
        int used = 0;
        if (userId != null) {
            used = drawMapper.selectCount(new LambdaQueryWrapper<LotteryDrawRecord>()
                    .eq(LotteryDrawRecord::getActivityId, a.getId()).eq(LotteryDrawRecord::getUserId, userId)
                    .eq(LotteryDrawRecord::getDrawDate, LocalDate.now())).intValue();
        }
        v.setUsedToday(used); v.setRemainingChances(Math.max(0, a.getDailyChances() - used));
        if (includePrizes) {
            v.setPrizes(prizeMapper.selectList(new LambdaQueryWrapper<LotteryPrize>()
                    .eq(LotteryPrize::getActivityId, a.getId()).orderByAsc(LotteryPrize::getSort)
                    .orderByAsc(LotteryPrize::getId)).stream().map(this::toPrizeVO).toList());
        }
        return v;
    }
    private LotteryPrizeVO toPrizeVO(LotteryPrize p) { LotteryPrizeVO v = new LotteryPrizeVO(); v.setId(p.getId()); v.setName(p.getName()); v.setPrizeType(p.getPrizeType()); v.setImage(p.getImage()); v.setPointsAmount(p.getPointsAmount()); v.setCouponTemplateId(p.getCouponTemplateId()); v.setProductId(p.getProductId()); v.setSkuId(p.getSkuId()); v.setTotalStock(p.getTotalStock()); v.setRemainingStock(p.getRemainingStock()); v.setProbability(p.getProbability()); v.setValidityDays(p.getValidityDays()); v.setSort(p.getSort()); v.setStatus(p.getStatus()); return v; }
    private LotteryDrawVO toDrawVO(LotteryDrawRecord r) { LotteryReward reward = rewardMapper.selectOne(new LambdaQueryWrapper<LotteryReward>().eq(LotteryReward::getDrawRecordId, r.getId())); return toDrawVO(r, reward); }
    private LotteryDrawVO toDrawVO(LotteryDrawRecord r, LotteryReward reward) { LotteryDrawVO v = new LotteryDrawVO(); v.setCreatedAt(r.getCreatedAt()); LotteryPrize p = prizeMapper.selectById(r.getPrizeId()); v.setPrize(p == null ? null : toPrizeVO(p)); if (v.getPrize() == null) { LotteryPrizeVO pv = new LotteryPrizeVO(); pv.setId(r.getPrizeId()); pv.setName(r.getPrizeName()); pv.setPrizeType(r.getPrizeType()); pv.setProbability(r.getProbabilitySnapshot()); v.setPrize(pv); } LotteryActivity a = activityMapper.selectById(r.getActivityId()); if (a != null) v.setActivity(toActivityVO(a, r.getUserId(), false)); if (reward != null) v.setReward(toRewardVO(reward)); v.setDrawNo(r.getDrawNo()); return v; }
    private LotteryRewardVO toRewardVO(LotteryReward r) { LotteryRewardVO v = new LotteryRewardVO(); v.setId(r.getId()); v.setDrawRecordId(r.getDrawRecordId()); v.setActivityId(r.getActivityId()); v.setPrizeId(r.getPrizeId()); v.setPrizeName(r.getPrizeName()); v.setPrizeType(r.getPrizeType()); v.setPointsAmount(r.getPointsAmount()); v.setCouponId(r.getCouponId()); v.setOrderNo(r.getOrderNo()); v.setStatus(r.getStatus()); v.setStatusText(switch (Optional.ofNullable(r.getStatus()).orElse(6)) { case 1 -> "已到账"; case 2 -> "待填写地址"; case 3 -> "待发货"; case 4 -> "已发货"; case 5 -> "已完成"; case 6 -> "发放失败"; default -> "处理中"; }); v.setFailureReason(r.getFailureReason()); v.setIssuedAt(r.getIssuedAt()); v.setClaimedAt(r.getClaimedAt()); v.setCreatedAt(r.getCreatedAt()); LotteryPrize p = prizeMapper.selectById(r.getPrizeId()); if (p != null) v.setImage(p.getImage()); LotteryActivity a = activityMapper.selectById(r.getActivityId()); if (a != null) v.setActivityName(a.getName()); return v; }
}
