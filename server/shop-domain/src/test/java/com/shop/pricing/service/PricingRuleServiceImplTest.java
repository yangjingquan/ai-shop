package com.shop.pricing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.pricing.dto.PricingRuleVersionRequest;
import com.shop.pricing.entity.PricingRuleVersion;
import com.shop.pricing.mapper.MarketingRuleAuditMapper;
import com.shop.pricing.mapper.PricingRuleVersionMapper;
import com.shop.pricing.service.impl.PricingRuleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingRuleServiceImplTest {
    @Mock private PricingRuleVersionMapper ruleMapper;
    @Mock private MarketingRuleAuditMapper auditMapper;

    @Test
    void usesCurrentMatrixToRejectAnExplicitlyBlockedCombination() {
        when(ruleMapper.selectOne(any())).thenReturn(rule("{\"SECKILL\":[\"COUPON\"]}", "[]"));
        PricingRuleServiceImpl service = service();
        assertThat(service.allowsCombination("SECKILL", "COUPON")).isFalse();
        assertThat(service.allowsCombination("SECKILL", "POINTS")).isTrue();
    }

    @Test
    void refusesAnInvalidRuleConfigurationBeforePublishing() {
        PricingRuleVersionRequest request = new PricingRuleVersionRequest();
        request.setMatrixJson("[]"); request.setZeroPayWhitelistJson("{}"); request.setMaxDiscountRate(new BigDecimal("1.01"));
        assertThatThrownBy(() -> service().publish(1L, request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("营销互斥矩阵必须是 JSON 对象");
    }

    private PricingRuleServiceImpl service() { return new PricingRuleServiceImpl(ruleMapper, auditMapper, new ObjectMapper()); }
    private PricingRuleVersion rule(String matrix, String whitelist) { PricingRuleVersion rule = new PricingRuleVersion(); rule.setVersion(1L); rule.setMatrixJson(matrix); rule.setZeroPayWhitelistJson(whitelist); rule.setMaxDiscountRate(BigDecimal.ONE); return rule; }
}
