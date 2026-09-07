package com.shop.pricing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.pricing.dto.PricingRuleVersionRequest;
import com.shop.pricing.dto.PricingRuleVersionVO;
import com.shop.pricing.entity.MarketingRuleAudit;
import com.shop.pricing.entity.PricingRuleVersion;
import com.shop.pricing.mapper.MarketingRuleAuditMapper;
import com.shop.pricing.mapper.PricingRuleVersionMapper;
import com.shop.pricing.service.PricingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PricingRuleServiceImpl implements PricingRuleService {
    private final PricingRuleVersionMapper ruleMapper;
    private final MarketingRuleAuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PricingRuleVersion active() {
        PricingRuleVersion rule = ruleMapper.selectOne(new LambdaQueryWrapper<PricingRuleVersion>()
                .eq(PricingRuleVersion::getStatus, 1).orderByDesc(PricingRuleVersion::getVersion).last("LIMIT 1"));
        if (rule == null) throw new BusinessException(ErrorCode.PRICING_RULE_UNAVAILABLE);
        return rule;
    }

    @Override public PricingRuleVersionVO activeView() { return view(active()); }

    @Override
    @Transactional
    public PricingRuleVersionVO publish(Long operatorId, PricingRuleVersionRequest request) {
        JsonNode matrix = parseJson(request.getMatrixJson(), "营销互斥矩阵");
        JsonNode whitelist = parseJson(request.getZeroPayWhitelistJson(), "零价白名单");
        if (!matrix.isObject()) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "营销互斥矩阵必须是 JSON 对象");
        if (!whitelist.isArray()) throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "零价白名单必须是 JSON 数组");
        BigDecimal maxDiscountRate = request.getMaxDiscountRate() == null ? BigDecimal.ONE : request.getMaxDiscountRate();
        if (maxDiscountRate.compareTo(BigDecimal.ZERO) < 0 || maxDiscountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "最大优惠比例必须在 0 到 1 之间");
        }
        PricingRuleVersion previous = active();
        PricingRuleVersion next = new PricingRuleVersion();
        next.setVersion(previous.getVersion() + 1);
        next.setMatrixJson(request.getMatrixJson());
        next.setZeroPayWhitelistJson(request.getZeroPayWhitelistJson());
        next.setMaxDiscountRate(maxDiscountRate);
        next.setStatus(1);
        next.setApprovedBy(operatorId);
        next.setApprovedAt(LocalDateTime.now());
        previous.setStatus(2);
        ruleMapper.updateById(previous);
        ruleMapper.insert(next);
        MarketingRuleAudit audit = new MarketingRuleAudit();
        audit.setRuleVersion(next.getVersion()); audit.setOperatorId(operatorId); audit.setAction("PUBLISH");
        audit.setReason(request.getReason() == null ? "" : request.getReason().trim());
        audit.setBeforeJson(previous.getMatrixJson()); audit.setAfterJson(next.getMatrixJson());
        auditMapper.insert(audit);
        return view(next);
    }

    @Override
    public boolean allowsZeroPay(String scene) {
        try {
            JsonNode whitelist = objectMapper.readTree(active().getZeroPayWhitelistJson());
            if (!whitelist.isArray()) return false;
            for (JsonNode node : whitelist) if (scene.equals(node.asText())) return true;
            return false;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PRICING_RULE_UNAVAILABLE.getCode(), "零价白名单配置无效");
        }
    }

    @Override
    public boolean allowsCombination(String primaryRule, String secondaryRule) {
        try {
            JsonNode blockedRules = objectMapper.readTree(active().getMatrixJson()).path(primaryRule);
            if (!blockedRules.isArray()) return true;
            for (JsonNode rule : blockedRules) if (secondaryRule.equals(rule.asText())) return false;
            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PRICING_RULE_UNAVAILABLE.getCode(), "营销互斥矩阵配置无效");
        }
    }

    private JsonNode parseJson(String value, String label) {
        try { return objectMapper.readTree(value); }
        catch (Exception e) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), label + "必须是有效 JSON"); }
    }
    private PricingRuleVersionVO view(PricingRuleVersion entity) {
        PricingRuleVersionVO vo = new PricingRuleVersionVO(); vo.setVersion(entity.getVersion());
        vo.setMatrixJson(entity.getMatrixJson()); vo.setZeroPayWhitelistJson(entity.getZeroPayWhitelistJson());
        vo.setMaxDiscountRate(entity.getMaxDiscountRate()); return vo;
    }
}
