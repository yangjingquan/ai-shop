package com.shop.pricing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.pricing.dto.QuoteRequest;
import com.shop.pricing.dto.QuoteResult;
import com.shop.pricing.entity.PriceQuote;
import com.shop.pricing.entity.PricingRuleVersion;
import com.shop.pricing.mapper.PriceQuoteMapper;
import com.shop.pricing.service.PricingRuleService;
import com.shop.pricing.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** I0-01 final-price authority. Callers resolve valid SKU/activity context, this service owns final arithmetic and snapshot validity. */
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {
    private static final int QUOTE_TTL_MINUTES = 10;
    private final PriceQuoteMapper quoteMapper;
    private final PricingRuleService pricingRuleService;
    private final ObjectMapper objectMapper;

    @Override
    public QuoteResult quote(QuoteRequest request) {
        if (request == null || request.getUserId() == null || request.getMerchantId() == null || blank(request.getScene())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        PricingRuleVersion rule = pricingRuleService.active();
        BigDecimal original = amount(request.getOriginalAmount(), "原价");
        BigDecimal activity = nonNegative(request.getActivityDiscountAmount(), "活动优惠");
        BigDecimal coupon = nonNegative(request.getCouponDiscountAmount(), "优惠券优惠");
        BigDecimal points = nonNegative(request.getPointsDiscountAmount(), "积分优惠");
        BigDecimal freight = nonNegative(request.getFreightAmount(), "运费");
        if (coupon.compareTo(BigDecimal.ZERO) > 0 && !pricingRuleService.allowsCombination(request.getScene(), "COUPON")) {
            throw new BusinessException(ErrorCode.PRICE_INVALID.getCode(), "当前活动不可与优惠券叠加");
        }
        if (points.compareTo(BigDecimal.ZERO) > 0 && !pricingRuleService.allowsCombination(request.getScene(), "POINTS")) {
            throw new BusinessException(ErrorCode.PRICE_INVALID.getCode(), "当前活动不可与积分抵扣叠加");
        }
        BigDecimal discount = activity.add(coupon).add(points);
        if (discount.compareTo(original) > 0 || discount.compareTo(original.multiply(rule.getMaxDiscountRate())) > 0) {
            throw new BusinessException(ErrorCode.PRICE_INVALID.getCode(), "优惠金额超过平台上限");
        }
        BigDecimal payable = original.add(freight).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (payable.compareTo(BigDecimal.ZERO) < 0 || (payable.compareTo(BigDecimal.ZERO) == 0 && !pricingRuleService.allowsZeroPay(request.getScene()))) {
            throw new BusinessException(ErrorCode.PRICE_INVALID.getCode(), "非白名单订单实付必须大于 0");
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(QUOTE_TTL_MINUTES);
        String snapshot = snapshot(request, rule.getVersion(), original, activity, coupon, points, freight, payable, expiresAt);
        PriceQuote entity = new PriceQuote();
        entity.setId("Q" + UUID.randomUUID().toString().replace("-", "")); entity.setUserId(request.getUserId());
        entity.setMerchantId(request.getMerchantId()); entity.setScene(request.getScene()); entity.setRuleVersion(rule.getVersion());
        entity.setOriginalAmount(original); entity.setActivityDiscountAmount(activity); entity.setCouponDiscountAmount(coupon);
        entity.setPointsDiscountAmount(points); entity.setFreightAmount(freight); entity.setPayableAmount(payable);
        entity.setCouponId(request.getCouponId()); entity.setSnapshotJson(snapshot); entity.setExpiresAt(expiresAt);
        quoteMapper.insert(entity);
        return result(entity, List.of(), snapshot);
    }

    @Override
    public QuoteResult requireValid(Long userId, Long merchantId, String quoteId, Long ruleVersion, String scene) {
        if (blank(quoteId) || ruleVersion == null) throw new BusinessException(ErrorCode.QUOTE_REQUIRED);
        PriceQuote quote = quoteMapper.selectOne(new LambdaQueryWrapper<PriceQuote>().eq(PriceQuote::getId, quoteId)
                .eq(PriceQuote::getUserId, userId).eq(PriceQuote::getMerchantId, merchantId).last("LIMIT 1"));
        if (quote == null || quote.getExpiresAt().isBefore(LocalDateTime.now()) || !ruleVersion.equals(quote.getRuleVersion())
                || !scene.equals(quote.getScene()) || !ruleVersion.equals(pricingRuleService.active().getVersion())) {
            throw new BusinessException(ErrorCode.QUOTE_EXPIRED);
        }
        return result(quote, List.of(), quote.getSnapshotJson());
    }

    private QuoteResult result(PriceQuote quote, List<String> reasons, String snapshot) {
        QuoteResult result = new QuoteResult(); result.setQuoteId(quote.getId()); result.setRuleVersion(quote.getRuleVersion());
        result.setScene(quote.getScene()); result.setOriginalAmount(quote.getOriginalAmount());
        result.setActivityDiscountAmount(quote.getActivityDiscountAmount()); result.setCouponDiscountAmount(quote.getCouponDiscountAmount());
        result.setPointsDiscountAmount(quote.getPointsDiscountAmount()); result.setFreightAmount(quote.getFreightAmount());
        result.setPayableAmount(quote.getPayableAmount()); result.setUnavailableReasons(reasons); result.setExpiresAt(quote.getExpiresAt());
        result.setPricingSnapshotJson(snapshot); return result;
    }
    private String snapshot(QuoteRequest request, Long version, BigDecimal original, BigDecimal activity, BigDecimal coupon,
                            BigDecimal points, BigDecimal freight, BigDecimal payable, LocalDateTime expiresAt) {
        try {
            Map<String, Object> values = new LinkedHashMap<>(); values.put("ruleVersion", version); values.put("scene", request.getScene());
            values.put("originalAmount", original); values.put("activityDiscountAmount", activity); values.put("couponDiscountAmount", coupon);
            values.put("pointsDiscountAmount", points); values.put("freightAmount", freight); values.put("payableAmount", payable);
            values.put("activityName", request.getActivityName() == null ? "" : request.getActivityName());
            values.put("items", request.getItems() == null ? List.of() : request.getItems()); values.put("expiresAt", expiresAt.toString());
            return objectMapper.writeValueAsString(values);
        }
        catch (Exception e) { throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "报价快照生成失败"); }
    }
    private BigDecimal amount(BigDecimal value, String name) { if (value == null || value.scale() > 2 || value.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(ErrorCode.PRICE_INVALID.getCode(), name + "不合法"); return value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal nonNegative(BigDecimal value, String name) { return amount(value == null ? BigDecimal.ZERO : value, name); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
