package com.shop.referral.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.coupon.entity.CouponTemplate;
import com.shop.coupon.entity.UserCoupon;
import com.shop.coupon.mapper.CouponTemplateMapper;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.coupon.service.CouponService;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.service.MarketingFeatureService;
import com.shop.order.entity.Order;
import com.shop.order.enums.OrderStatus;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.dto.ProductDetailVO;
import com.shop.product.service.ProductService;
import com.shop.referral.dto.*;
import com.shop.referral.entity.*;
import com.shop.referral.enums.ReferralCampaignStatus;
import com.shop.referral.enums.ReferralRelationStatus;
import com.shop.referral.enums.ReferralRewardStatus;
import com.shop.referral.mapper.*;
import com.shop.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {
    private static final int TIER_INVITEE = 0;
    private static final String ROLE_INVITER = "INVITER";
    private static final String ROLE_INVITEE = "INVITEE";
    private static final String EVENT_SHARE = "SHARE";
    private static final String EVENT_OPEN = "OPEN";
    private static final String EVENT_REGISTER = "REGISTER";
    private static final String EVENT_FIRST_PURCHASE = "FIRST_PURCHASE";
    private static final String EVENT_REWARD_ISSUED = "REWARD_ISSUED";

    private final ReferralCampaignMapper campaignMapper;
    private final ReferralRelationMapper relationMapper;
    private final ReferralRewardMapper rewardMapper;
    private final ReferralShareTokenMapper tokenMapper;
    private final ReferralShareEventMapper eventMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderMapper orderMapper;
    private final ProductService productService;
    private final CouponService couponService;
    private final MarketingFeatureService marketingFeatureService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ReferralCampaignVO current(Long merchantId, Long userId, String token) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REFERRAL);
        ReferralCampaign campaign = campaignMapper.selectOne(new LambdaQueryWrapper<ReferralCampaign>()
                .eq(ReferralCampaign::getMerchantId, merchantId)
                .eq(ReferralCampaign::getStatus, ReferralCampaignStatus.ACTIVE.getCode())
                .le(ReferralCampaign::getStartAt, LocalDateTime.now())
                .ge(ReferralCampaign::getEndAt, LocalDateTime.now())
                .orderByDesc(ReferralCampaign::getId)
                .last("LIMIT 1"));
        // 首页和支付成功页会探测当前活动；没有进行中的活动时返回空数据，避免把正常的“未配置/已结束”状态提示成错误。
        if (campaign == null) return null;
        if (token != null && !token.isBlank()) {
            resolveToken(merchantId, campaign.getId(), token);
            recordEvent(campaign, EVENT_OPEN, null, userId, token, userId, null);
        }
        return buildVO(campaign, userId, token, true);
    }

    @Override
    public ReferralCampaignVO get(Long merchantId, Long userId, Long campaignId, String token) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REFERRAL);
        ReferralCampaign campaign = ownedCampaign(merchantId, campaignId);
        if (token != null && !token.isBlank()) {
            resolveToken(merchantId, campaignId, token);
            recordEvent(campaign, EVENT_OPEN, null, userId, token, null, null);
        }
        if (!isActive(campaign)) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请活动已结束或暂未开始");
        return buildVO(campaign, userId, token, true);
    }

    @Override
    @Transactional
    public String share(Long merchantId, Long userId, Long campaignId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REFERRAL);
        ReferralCampaign campaign = activeCampaign(merchantId, campaignId);
        ReferralShareToken token = tokenMapper.selectOne(new LambdaQueryWrapper<ReferralShareToken>()
                .eq(ReferralShareToken::getCampaignId, campaignId)
                .eq(ReferralShareToken::getInviterUserId, userId));
        if (token == null || (token.getExpiresAt() != null && token.getExpiresAt().isBefore(campaign.getEndAt()))) {
            token = new ReferralShareToken();
            token.setCampaignId(campaignId);
            token.setMerchantId(merchantId);
            token.setInviterUserId(userId);
            token.setToken(UUID.randomUUID().toString().replace("-", ""));
            token.setExpiresAt(campaign.getEndAt());
            tokenMapper.insert(token);
        }
        recordEvent(campaign, EVENT_SHARE, null, userId, token.getToken(), null, null);
        return token.getToken();
    }

    @Override
    @Transactional
    public ReferralCampaignVO bind(Long merchantId, Long userId, Long campaignId, String token) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REFERRAL);
        ReferralCampaign campaign = activeCampaign(merchantId, campaignId);
        ReferralShareToken shareToken = resolveToken(merchantId, campaignId, token);
        if (Objects.equals(shareToken.getInviterUserId(), userId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "不能邀请自己");
        }
        ReferralRelation existing = relationMapper.selectOne(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getCampaignId, campaignId)
                .eq(ReferralRelation::getInviteeUserId, userId));
        if (existing != null) return buildVO(campaign, userId, token, true);
        if (hasSuccessfulOrder(userId, merchantId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前账号已完成过首单，无法参与新人邀请");
        }
        long total = relationMapper.selectCount(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getCampaignId, campaignId)
                .eq(ReferralRelation::getInviterUserId, shareToken.getInviterUserId())
                .ne(ReferralRelation::getStatus, ReferralRelationStatus.INVALID.getCode()));
        if (campaign.getMaxTotalInvites() != null && campaign.getMaxTotalInvites() > 0 && total >= campaign.getMaxTotalInvites()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "该邀请人的活动奖励已达上限");
        }
        if (campaign.getMaxDailyInvites() != null && campaign.getMaxDailyInvites() > 0) {
            LocalDateTime dayStart = LocalDate.now().atStartOfDay();
            long today = relationMapper.selectCount(new LambdaQueryWrapper<ReferralRelation>()
                    .eq(ReferralRelation::getCampaignId, campaignId)
                    .eq(ReferralRelation::getInviterUserId, shareToken.getInviterUserId())
                    .ge(ReferralRelation::getBoundAt, dayStart)
                    .ne(ReferralRelation::getStatus, ReferralRelationStatus.INVALID.getCode()));
            if (today >= campaign.getMaxDailyInvites()) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "该邀请人今日名额已用完");
        }
        ReferralRelation relation = new ReferralRelation();
        relation.setCampaignId(campaignId);
        relation.setMerchantId(merchantId);
        relation.setInviterUserId(shareToken.getInviterUserId());
        relation.setInviteeUserId(userId);
        relation.setSourceToken(token);
        relation.setStatus(ReferralRelationStatus.BOUND.getCode());
        relation.setBoundAt(LocalDateTime.now());
        relationMapper.insert(relation);
        recordEvent(campaign, EVENT_REGISTER, relation, userId, token, userId, null);
        return buildVO(campaign, userId, token, true);
    }

    @Override
    @Transactional
    public Long claimInviteeCoupon(Long merchantId, Long userId, Long campaignId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REFERRAL);
        ReferralCampaign campaign = activeCampaign(merchantId, campaignId);
        ReferralRelation relation = relationMapper.selectOne(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getCampaignId, campaignId)
                .eq(ReferralRelation::getInviteeUserId, userId)
                .last("FOR UPDATE"));
        if (relation == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "请从邀请链接进入活动");
        if (relation.getStatus() != ReferralRelationStatus.BOUND.getCode()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前账号不符合新人券领取条件");
        }
        ReferralReward existing = rewardMapper.selectOne(new LambdaQueryWrapper<ReferralReward>()
                .eq(ReferralReward::getCampaignId, campaignId).eq(ReferralReward::getUserId, userId)
                .eq(ReferralReward::getRole, ROLE_INVITEE).eq(ReferralReward::getTier, TIER_INVITEE));
        if (existing != null) {
            if (existing.getStatus() == ReferralRewardStatus.ISSUED.getCode() && existing.getCouponId() != null) {
                return existing.getCouponId();
            }
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "新人券已领取或当前奖励不可重复领取");
        }
        if (campaign.getInviteeCouponTemplateId() == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "新人券尚未配置");
        Long couponId = couponService.issueTemplate(userId, merchantId, campaign.getInviteeCouponTemplateId());
        ReferralReward reward = new ReferralReward();
        reward.setCampaignId(campaignId); reward.setMerchantId(merchantId); reward.setRelationId(relation.getId());
        reward.setUserId(userId); reward.setRole(ROLE_INVITEE); reward.setTier(TIER_INVITEE);
        reward.setCouponTemplateId(campaign.getInviteeCouponTemplateId()); reward.setCouponId(couponId);
        reward.setStatus(ReferralRewardStatus.ISSUED.getCode()); reward.setIssuedAt(LocalDateTime.now());
        CouponTemplate template = couponTemplateMapper.selectById(campaign.getInviteeCouponTemplateId());
        reward.setRewardAmount(template == null ? BigDecimal.ZERO : template.getAmount());
        rewardMapper.insert(reward);
        recordEvent(campaign, EVENT_REWARD_ISSUED, relation, userId, relation.getSourceToken(), userId, null);
        return couponId;
    }

    @Override
    public List<ReferralRewardVO> userRewards(Long merchantId, Long userId, Long campaignId) {
        ownedCampaign(merchantId, campaignId);
        return rewardMapper.selectList(new LambdaQueryWrapper<ReferralReward>()
                        .eq(ReferralReward::getCampaignId, campaignId).eq(ReferralReward::getUserId, userId)
                        .orderByDesc(ReferralReward::getId))
                .stream().map(this::toRewardVO).toList();
    }

    @Override
    public List<ReferralCampaignVO> merchantList(Long merchantId) {
        return campaignMapper.selectList(new LambdaQueryWrapper<ReferralCampaign>()
                        .eq(ReferralCampaign::getMerchantId, merchantId).orderByDesc(ReferralCampaign::getId))
                .stream().map(c -> buildVO(c, null, null, false)).toList();
    }

    @Override
    public ReferralCampaignVO merchantGet(Long merchantId, Long campaignId) {
        return buildVO(ownedCampaign(merchantId, campaignId), null, null, false);
    }

    @Override
    @Transactional
    public Long create(Long merchantId, ReferralCampaignSaveRequest request) {
        validateSave(merchantId, request);
        ReferralCampaign campaign = new ReferralCampaign();
        applySave(campaign, request, merchantId);
        campaignMapper.insert(campaign);
        return campaign.getId();
    }

    @Override
    @Transactional
    public void update(Long merchantId, Long campaignId, ReferralCampaignSaveRequest request) {
        validateSave(merchantId, request);
        ReferralCampaign campaign = ownedCampaign(merchantId, campaignId);
        applySave(campaign, request, merchantId);
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional
    public void updateStatus(Long merchantId, Long campaignId, Integer status) {
        ReferralCampaign campaign = ownedCampaign(merchantId, campaignId);
        if (status == null || Arrays.stream(ReferralCampaignStatus.values()).noneMatch(s -> s.getCode() == status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        campaign.setStatus(status);
        campaignMapper.updateById(campaign);
    }

    @Override
    public ReferralStatsVO stats(Long merchantId, Long campaignId) {
        ownedCampaign(merchantId, campaignId);
        List<ReferralShareEvent> events = eventMapper.selectList(new LambdaQueryWrapper<ReferralShareEvent>()
                .eq(ReferralShareEvent::getMerchantId, merchantId).eq(ReferralShareEvent::getCampaignId, campaignId));
        ReferralStatsVO vo = new ReferralStatsVO();
        vo.setShares(events.stream().filter(e -> EVENT_SHARE.equals(e.getEventType())).count());
        vo.setOpens(events.stream().filter(e -> EVENT_OPEN.equals(e.getEventType())).count());
        vo.setRegistrations(events.stream().filter(e -> EVENT_REGISTER.equals(e.getEventType())).count());
        vo.setFirstPurchases(events.stream().filter(e -> EVENT_FIRST_PURCHASE.equals(e.getEventType())).count());
        List<ReferralReward> issued = rewardMapper.selectList(new LambdaQueryWrapper<ReferralReward>()
                .eq(ReferralReward::getCampaignId, campaignId).eq(ReferralReward::getStatus, ReferralRewardStatus.ISSUED.getCode()));
        vo.setRewardsIssued(issued.size());
        vo.setRewardCost(issued.stream().map(ReferralReward::getRewardAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        return vo;
    }

    @Override
    public List<ReferralRelationVO> relations(Long merchantId, Long campaignId) {
        ownedCampaign(merchantId, campaignId);
        return relationMapper.selectList(new LambdaQueryWrapper<ReferralRelation>()
                        .eq(ReferralRelation::getMerchantId, merchantId).eq(ReferralRelation::getCampaignId, campaignId)
                        .orderByDesc(ReferralRelation::getId))
                .stream().map(this::toRelationVO).toList();
    }

    @Override
    public List<ReferralRewardVO> rewards(Long merchantId, Long campaignId) {
        ownedCampaign(merchantId, campaignId);
        return rewardMapper.selectList(new LambdaQueryWrapper<ReferralReward>()
                        .eq(ReferralReward::getMerchantId, merchantId).eq(ReferralReward::getCampaignId, campaignId)
                        .orderByDesc(ReferralReward::getId))
                .stream().map(this::toRewardVO).toList();
    }

    @Override
    @Transactional
    public void freezeRelation(Long merchantId, Long campaignId, Long relationId) {
        ReferralRelation relation = relationMapper.selectOne(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getId, relationId).eq(ReferralRelation::getMerchantId, merchantId)
                .eq(ReferralRelation::getCampaignId, campaignId).last("FOR UPDATE"));
        if (relation == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请关系不存在");
        relation.setStatus(ReferralRelationStatus.FROZEN.getCode());
        relationMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void revokeReward(Long merchantId, Long campaignId, Long rewardId, String reason) {
        ReferralReward reward = rewardMapper.selectOne(new LambdaQueryWrapper<ReferralReward>()
                .eq(ReferralReward::getId, rewardId).eq(ReferralReward::getMerchantId, merchantId)
                .eq(ReferralReward::getCampaignId, campaignId).last("FOR UPDATE"));
        if (reward == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "奖励不存在");
        if (reward.getStatus() != ReferralRewardStatus.ISSUED.getCode()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前奖励不可撤销");
        }
        if (reward.getCouponId() != null && !couponService.invalidateCoupon(reward.getUserId(), merchantId, reward.getCouponId())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "优惠券已使用，不能撤销");
        }
        reward.setStatus(ReferralRewardStatus.REVOKED.getCode());
        reward.setRevokeReason(reason == null || reason.isBlank() ? "后台撤销" : reason.trim());
        reward.setRevokedAt(LocalDateTime.now());
        rewardMapper.updateById(reward);
    }

    @Override
    @Transactional
    public void handleOrderPaid(Order order) {
        if (order == null || order.getUserId() == null || order.getMerchantId() == null || order.getPayTime() == null) return;
        List<ReferralRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getMerchantId, order.getMerchantId())
                .eq(ReferralRelation::getInviteeUserId, order.getUserId())
                .eq(ReferralRelation::getStatus, ReferralRelationStatus.BOUND.getCode()));
        for (ReferralRelation relation : relations) {
            // 以活动行作为同一邀请人的结算锁，避免并发首购同时发放同一阶梯奖励。
            ReferralCampaign campaign = campaignMapper.selectOne(new LambdaQueryWrapper<ReferralCampaign>()
                    .eq(ReferralCampaign::getId, relation.getCampaignId()).last("FOR UPDATE"));
            if (campaign == null) continue;
            relation.setFirstOrderNo(order.getOrderNo());
            relation.setCompletedAt(LocalDateTime.now());
            relation.setStatus(ReferralRelationStatus.COMPLETED.getCode());
            relationMapper.updateById(relation);
            recordEvent(campaign, EVENT_FIRST_PURCHASE, relation, order.getUserId(), relation.getSourceToken(), order.getUserId(), order.getOrderNo());
            int completedCount = relationMapper.selectList(new LambdaQueryWrapper<ReferralRelation>()
                    .eq(ReferralRelation::getCampaignId, campaign.getId())
                    .eq(ReferralRelation::getInviterUserId, relation.getInviterUserId())
                    .eq(ReferralRelation::getStatus, ReferralRelationStatus.COMPLETED.getCode())
                    .last("FOR UPDATE")).size();
            for (ReferralTierRequest tier : tiers(campaign)) {
                if (completedCount >= tier.getInviteCount()) issueInviterReward(campaign, relation, tier, order.getOrderNo());
            }
        }
    }

    @Override
    @Transactional
    public void handleOrderRefunded(String orderNo) {
        ReferralRelation relation = relationMapper.selectOne(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getFirstOrderNo, orderNo).last("LIMIT 1"));
        if (relation == null) return;
        relation.setStatus(ReferralRelationStatus.REFUND_PENDING.getCode());
        relationMapper.updateById(relation);
        List<ReferralReward> rewards = rewardMapper.selectList(new LambdaQueryWrapper<ReferralReward>()
                .eq(ReferralReward::getRelationId, relation.getId()).eq(ReferralReward::getStatus, ReferralRewardStatus.ISSUED.getCode()));
        for (ReferralReward reward : rewards) {
            if (reward.getCouponId() == null || couponService.invalidateCoupon(reward.getUserId(), relation.getMerchantId(), reward.getCouponId())) {
                reward.setStatus(ReferralRewardStatus.REVOKED.getCode());
                reward.setRevokeReason("好友首单退款");
                reward.setRevokedAt(LocalDateTime.now());
            } else {
                reward.setStatus(ReferralRewardStatus.FROZEN.getCode());
                reward.setRevokeReason("好友首单退款，优惠券已使用");
            }
            rewardMapper.updateById(reward);
        }
    }

    private void issueInviterReward(ReferralCampaign campaign, ReferralRelation relation, ReferralTierRequest tier, String orderNo) {
        ReferralReward existing = rewardMapper.selectOne(new LambdaQueryWrapper<ReferralReward>()
                .eq(ReferralReward::getCampaignId, campaign.getId()).eq(ReferralReward::getUserId, relation.getInviterUserId())
                .eq(ReferralReward::getRole, ROLE_INVITER).eq(ReferralReward::getTier, tier.getInviteCount())
                .last("FOR UPDATE"));
        if (existing != null && (existing.getStatus() == ReferralRewardStatus.ISSUED.getCode()
                || existing.getStatus() == ReferralRewardStatus.REVOKED.getCode()
                || existing.getStatus() == ReferralRewardStatus.FROZEN.getCode())) return;
        ReferralReward reward = existing == null ? new ReferralReward() : existing;
        reward.setCampaignId(campaign.getId()); reward.setMerchantId(campaign.getMerchantId()); reward.setRelationId(relation.getId());
        reward.setUserId(relation.getInviterUserId()); reward.setRole(ROLE_INVITER); reward.setTier(tier.getInviteCount());
        reward.setCouponTemplateId(tier.getInviterCouponTemplateId()); reward.setTriggerOrderNo(orderNo);
        try {
            Long couponId = couponService.issueTemplate(relation.getInviterUserId(), campaign.getMerchantId(), tier.getInviterCouponTemplateId());
            reward.setCouponId(couponId); reward.setStatus(ReferralRewardStatus.ISSUED.getCode()); reward.setIssuedAt(LocalDateTime.now());
            CouponTemplate template = couponTemplateMapper.selectById(tier.getInviterCouponTemplateId());
            reward.setRewardAmount(template == null ? BigDecimal.ZERO : template.getAmount());
            if (reward.getId() == null) rewardMapper.insert(reward); else rewardMapper.updateById(reward);
            recordEvent(campaign, EVENT_REWARD_ISSUED, relation, relation.getInviterUserId(), relation.getSourceToken(), relation.getInviterUserId(), orderNo);
        } catch (RuntimeException ex) {
            reward.setStatus(ReferralRewardStatus.FAILED.getCode()); reward.setFailureReason(ex.getMessage());
            if (reward.getId() == null) rewardMapper.insert(reward); else rewardMapper.updateById(reward);
            log.warn("邀请奖励发放失败 campaignId={}, relationId={}", campaign.getId(), relation.getId(), ex);
        }
    }

    private ReferralCampaign activeCampaign(Long merchantId, Long campaignId) {
        marketingFeatureService.assertEnabled(merchantId, MarketingActivityCode.REFERRAL);
        ReferralCampaign campaign = ownedCampaign(merchantId, campaignId);
        if (!isActive(campaign)) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请活动已结束或暂未开始");
        return campaign;
    }

    private ReferralCampaign ownedCampaign(Long merchantId, Long campaignId) {
        ReferralCampaign campaign = campaignMapper.selectOne(new LambdaQueryWrapper<ReferralCampaign>()
                .eq(ReferralCampaign::getId, campaignId).eq(ReferralCampaign::getMerchantId, merchantId));
        if (campaign == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请活动不存在");
        return campaign;
    }

    private ReferralShareToken resolveToken(Long merchantId, Long campaignId, String value) {
        if (value == null || value.isBlank()) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请链接已失效");
        ReferralShareToken token = tokenMapper.selectOne(new LambdaQueryWrapper<ReferralShareToken>()
                .eq(ReferralShareToken::getToken, value).eq(ReferralShareToken::getMerchantId, merchantId)
                .eq(ReferralShareToken::getCampaignId, campaignId));
        if (token == null || (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请链接已失效");
        }
        return token;
    }

    private boolean isActive(ReferralCampaign campaign) {
        LocalDateTime now = LocalDateTime.now();
        return ReferralCampaignStatus.ACTIVE.getCode() == campaign.getStatus()
                && campaign.getStartAt() != null && !now.isBefore(campaign.getStartAt())
                && campaign.getEndAt() != null && !now.isAfter(campaign.getEndAt());
    }

    private ReferralCampaignVO buildVO(ReferralCampaign campaign, Long userId, String token, boolean userView) {
        ReferralCampaignVO vo = new ReferralCampaignVO();
        vo.setId(campaign.getId()); vo.setName(campaign.getName()); vo.setShareTitle(campaign.getShareTitle());
        vo.setShareDescription(campaign.getShareDescription()); vo.setLandingProductId(campaign.getLandingProductId());
        vo.setInviteeCouponTemplateId(campaign.getInviteeCouponTemplateId()); vo.setStartAt(campaign.getStartAt()); vo.setEndAt(campaign.getEndAt());
        vo.setMaxDailyInvites(campaign.getMaxDailyInvites()); vo.setMaxTotalInvites(campaign.getMaxTotalInvites());
        vo.setStatus(campaign.getStatus()); vo.setStatusText(statusText(campaign.getStatus())); vo.setActive(isActive(campaign));
        if (campaign.getLandingProductId() != null) {
            try {
                ProductDetailVO product = productService.publicGet(campaign.getLandingProductId(), campaign.getMerchantId());
                vo.setLandingProductName(product.getName()); vo.setLandingProductImage(product.getMainImage());
                vo.setLandingProductPrice(product.getMinPrice() == null ? "0.00" : product.getMinPrice().setScale(2).toPlainString());
            } catch (RuntimeException ignored) { }
        }
        CouponTemplate inviteeTemplate = couponTemplateMapper.selectById(campaign.getInviteeCouponTemplateId());
        if (inviteeTemplate != null) {
            vo.setInviteeCouponName(inviteeTemplate.getName());
            vo.setInviteeCouponAmount(inviteeTemplate.getAmount() == null ? "0.00" : inviteeTemplate.getAmount().setScale(2).toPlainString());
        }
        List<ReferralTierRequest> tiers = tiers(campaign);
        Map<Integer, ReferralReward> rewarded = userId == null ? Map.of() : rewardMapper.selectList(new LambdaQueryWrapper<ReferralReward>()
                        .eq(ReferralReward::getCampaignId, campaign.getId()).eq(ReferralReward::getUserId, userId).eq(ReferralReward::getRole, ROLE_INVITER))
                .stream().collect(Collectors.toMap(ReferralReward::getTier, r -> r, (a, b) -> a));
        long completed = userId == null ? 0 : relationMapper.selectCount(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getCampaignId, campaign.getId()).eq(ReferralRelation::getInviterUserId, userId)
                .eq(ReferralRelation::getStatus, ReferralRelationStatus.COMPLETED.getCode()));
        long pending = userId == null ? 0 : relationMapper.selectCount(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getCampaignId, campaign.getId()).eq(ReferralRelation::getInviterUserId, userId)
                .eq(ReferralRelation::getStatus, ReferralRelationStatus.BOUND.getCode()));
        List<ReferralTierVO> tierVOs = tiers.stream().map(t -> {
            ReferralTierVO tier = new ReferralTierVO(); tier.setInviteCount(t.getInviteCount()); tier.setInviterCouponTemplateId(t.getInviterCouponTemplateId());
            CouponTemplate template = couponTemplateMapper.selectById(t.getInviterCouponTemplateId());
            if (template != null) { tier.setCouponName(template.getName()); tier.setCouponAmount(template.getAmount().setScale(2).toPlainString()); }
            tier.setReached(completed >= t.getInviteCount());
            tier.setRewarded(rewarded.containsKey(t.getInviteCount()) && rewarded.get(t.getInviteCount()).getStatus() == ReferralRewardStatus.ISSUED.getCode());
            return tier;
        }).toList();
        vo.setTiers(tierVOs); vo.setCompletedInviteCount((int) completed); vo.setPendingInviteCount((int) pending);
        tiers.stream().filter(t -> completed < t.getInviteCount()).findFirst().ifPresent(next -> {
            vo.setNextTierInviteCount(next.getInviteCount()); vo.setRemainingToNextTier(next.getInviteCount() - (int) completed);
        });
        if (userView && token != null && !token.isBlank()) {
            ReferralShareToken shareToken = tokenMapper.selectOne(new LambdaQueryWrapper<ReferralShareToken>().eq(ReferralShareToken::getToken, token));
            boolean invitee = shareToken != null && !Objects.equals(shareToken.getInviterUserId(), userId);
            vo.setInvitee(invitee);
            vo.setOldUser(userId != null && hasSuccessfulOrder(userId, campaign.getMerchantId()));
            if (invitee && userId != null) {
                ReferralReward reward = rewardMapper.selectOne(new LambdaQueryWrapper<ReferralReward>()
                        .eq(ReferralReward::getCampaignId, campaign.getId()).eq(ReferralReward::getUserId, userId)
                        .eq(ReferralReward::getRole, ROLE_INVITEE).eq(ReferralReward::getTier, TIER_INVITEE));
                vo.setCanClaimInviteeCoupon(reward == null && !Boolean.TRUE.equals(vo.getOldUser()));
                vo.setInviteeCouponId(reward == null ? null : reward.getCouponId());
            }
        }
        if (userId != null && (token == null || token.isBlank())) {
            ReferralShareToken shareToken = tokenMapper.selectOne(new LambdaQueryWrapper<ReferralShareToken>()
                    .eq(ReferralShareToken::getCampaignId, campaign.getId()).eq(ReferralShareToken::getInviterUserId, userId));
            if (shareToken != null) vo.setShareToken(shareToken.getToken());
        }
        return vo;
    }

    private List<ReferralTierRequest> tiers(ReferralCampaign campaign) {
        try { return objectMapper.readValue(campaign.getTierConfigJson(), new TypeReference<List<ReferralTierRequest>>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private void validateSave(Long merchantId, ReferralCampaignSaveRequest request) {
        if (request.getStartAt().isAfter(request.getEndAt())) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "活动时间不合法");
        if (request.getStatus() == null || request.getStatus() < 0 || request.getStatus() > 3) throw new BusinessException(ErrorCode.PARAM_ERROR);
        List<Integer> counts = request.getTiers().stream().map(ReferralTierRequest::getInviteCount).sorted().toList();
        if (!new HashSet<>(counts).containsAll(List.of(1, 3, 5))) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "邀请阶梯必须包含1、3、5人");
        CouponTemplate invitee = couponTemplateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, request.getInviteeCouponTemplateId()).eq(CouponTemplate::getMerchantId, merchantId));
        if (invitee == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "好友新人券模板不存在");
        for (ReferralTierRequest tier : request.getTiers()) {
            CouponTemplate template = couponTemplateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                    .eq(CouponTemplate::getId, tier.getInviterCouponTemplateId()).eq(CouponTemplate::getMerchantId, merchantId));
            if (template == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "邀请人奖励券模板不存在");
        }
    }

    private void applySave(ReferralCampaign campaign, ReferralCampaignSaveRequest request, Long merchantId) {
        campaign.setMerchantId(merchantId); campaign.setName(request.getName().trim()); campaign.setShareTitle(request.getShareTitle().trim());
        campaign.setShareDescription(request.getShareDescription() == null ? "邀请好友完成首单，双方都能得券" : request.getShareDescription().trim());
        campaign.setLandingProductId(request.getLandingProductId()); campaign.setInviteeCouponTemplateId(request.getInviteeCouponTemplateId());
        try { campaign.setTierConfigJson(objectMapper.writeValueAsString(request.getTiers())); }
        catch (Exception e) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "奖励阶梯配置不合法"); }
        campaign.setStartAt(request.getStartAt()); campaign.setEndAt(request.getEndAt());
        campaign.setMaxDailyInvites(request.getMaxDailyInvites() == null ? 20 : Math.max(0, request.getMaxDailyInvites()));
        campaign.setMaxTotalInvites(request.getMaxTotalInvites() == null ? 0 : Math.max(0, request.getMaxTotalInvites()));
        campaign.setStatus(request.getStatus() == null ? ReferralCampaignStatus.DRAFT.getCode() : request.getStatus());
    }

    private boolean hasSuccessfulOrder(Long userId, Long merchantId) {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId).eq(Order::getMerchantId, merchantId)
                .in(Order::getStatus, List.of(OrderStatus.WAIT_SHIP.getCode(), OrderStatus.WAIT_RECEIVE.getCode(), OrderStatus.FINISHED.getCode(), OrderStatus.WAIT_GROUP.getCode(), OrderStatus.GROUP_SUCCESS.getCode()))
                .isNotNull(Order::getPayTime)) > 0;
    }

    private void recordEvent(ReferralCampaign campaign, String eventType, ReferralRelation relation, Long inviterUserId,
                             String token, Long inviteeUserId, String orderNo) {
        ReferralShareEvent event = new ReferralShareEvent(); event.setCampaignId(campaign.getId()); event.setMerchantId(campaign.getMerchantId());
        event.setRelationId(relation == null ? null : relation.getId()); event.setInviterUserId(inviterUserId);
        event.setInviteeUserId(inviteeUserId); event.setToken(token); event.setEventType(eventType); event.setOrderNo(orderNo);
        eventMapper.insert(event);
    }

    private ReferralRelationVO toRelationVO(ReferralRelation relation) {
        ReferralRelationVO vo = new ReferralRelationVO(); vo.setId(relation.getId()); vo.setInviterUserId(relation.getInviterUserId());
        vo.setInviteeUserId(relation.getInviteeUserId()); vo.setFirstOrderNo(relation.getFirstOrderNo()); vo.setStatus(relation.getStatus());
        vo.setStatusText(Arrays.stream(ReferralRelationStatus.values()).filter(s -> s.getCode() == relation.getStatus()).map(ReferralRelationStatus::getText).findFirst().orElse("未知"));
        vo.setBoundAt(relation.getBoundAt()); vo.setCompletedAt(relation.getCompletedAt()); return vo;
    }

    private ReferralRewardVO toRewardVO(ReferralReward reward) {
        ReferralRewardVO vo = new ReferralRewardVO(); vo.setId(reward.getId()); vo.setRelationId(reward.getRelationId()); vo.setUserId(reward.getUserId());
        vo.setRole(reward.getRole()); vo.setTier(reward.getTier()); vo.setCouponTemplateId(reward.getCouponTemplateId()); vo.setCouponId(reward.getCouponId());
        vo.setRewardAmount(reward.getRewardAmount()); vo.setTriggerOrderNo(reward.getTriggerOrderNo()); vo.setStatus(reward.getStatus());
        vo.setStatusText(Arrays.stream(ReferralRewardStatus.values()).filter(s -> s.getCode() == reward.getStatus()).map(ReferralRewardStatus::getText).findFirst().orElse("未知"));
        vo.setFailureReason(reward.getFailureReason()); vo.setRevokeReason(reward.getRevokeReason()); vo.setIssuedAt(reward.getIssuedAt()); vo.setRevokedAt(reward.getRevokedAt());
        return vo;
    }

    private String statusText(Integer status) {
        return Arrays.stream(ReferralCampaignStatus.values()).filter(s -> s.getCode() == status).map(ReferralCampaignStatus::getText).findFirst().orElse("未知");
    }
}
