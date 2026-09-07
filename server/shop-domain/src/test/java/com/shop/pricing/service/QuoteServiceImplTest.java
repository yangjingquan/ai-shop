package com.shop.pricing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.pricing.dto.QuoteRequest;
import com.shop.pricing.dto.QuoteResult;
import com.shop.pricing.entity.PriceQuote;
import com.shop.pricing.entity.PricingRuleVersion;
import com.shop.pricing.mapper.PriceQuoteMapper;
import com.shop.pricing.service.impl.QuoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceImplTest {
    @Mock private PriceQuoteMapper quoteMapper;
    @Mock private PricingRuleService pricingRuleService;

    @Test
    void createsVersionedFinalQuoteWithAllPriceComponents() {
        when(pricingRuleService.active()).thenReturn(rule(7L));
        when(pricingRuleService.allowsCombination("NORMAL", "COUPON")).thenReturn(true);

        QuoteRequest request = request("NORMAL", "100.00", "20.00", "10.00");
        QuoteResult result = service().quote(request);

        assertThat(result.getRuleVersion()).isEqualTo(7L);
        assertThat(result.getPayableAmount()).isEqualByComparingTo("70.00");
        assertThat(result.getQuoteId()).startsWith("Q");
        ArgumentCaptor<PriceQuote> quote = ArgumentCaptor.forClass(PriceQuote.class);
        verify(quoteMapper).insert(quote.capture());
        assertThat(quote.getValue().getSnapshotJson()).contains("ruleVersion", "activityDiscountAmount", "payableAmount");
    }

    @Test
    void rejectsZeroPayForNormalGoods() {
        when(pricingRuleService.active()).thenReturn(rule(1L));
        when(pricingRuleService.allowsZeroPay("NORMAL")).thenReturn(false);
        assertThatThrownBy(() -> service().quote(request("NORMAL", "10.00", "10.00", "0.00")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("实付必须大于 0");
    }

    @Test
    void permitsOnlyExplicitlyWhitelistedZeroPayScene() {
        when(pricingRuleService.active()).thenReturn(rule(1L));
        when(pricingRuleService.allowsZeroPay("POINTS_EXCHANGE")).thenReturn(true);
        assertThat(service().quote(request("POINTS_EXCHANGE", "10.00", "10.00", "0.00")).getPayableAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsAQuoteWhenThePlatformRuleVersionChanged() {
        PriceQuote quote = new PriceQuote(); quote.setId("Q1"); quote.setUserId(1L); quote.setMerchantId(2L); quote.setScene("NORMAL"); quote.setRuleVersion(1L);
        quote.setExpiresAt(LocalDateTime.now().plusMinutes(1)); quote.setOriginalAmount(new BigDecimal("10.00")); quote.setActivityDiscountAmount(BigDecimal.ZERO); quote.setCouponDiscountAmount(BigDecimal.ZERO); quote.setPointsDiscountAmount(BigDecimal.ZERO); quote.setFreightAmount(BigDecimal.ZERO); quote.setPayableAmount(new BigDecimal("10.00")); quote.setSnapshotJson("{}");
        when(quoteMapper.selectOne(any())).thenReturn(quote); when(pricingRuleService.active()).thenReturn(rule(2L));
        assertThatThrownBy(() -> service().requireValid(1L, 2L, "Q1", 1L, "NORMAL"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("报价已失效");
    }

    @Test
    void rejectsCouponStackingWhenTheRuleMatrixForbidsIt() {
        when(pricingRuleService.active()).thenReturn(rule(1L));
        when(pricingRuleService.allowsCombination("SECKILL", "COUPON")).thenReturn(false);
        assertThatThrownBy(() -> service().quote(request("SECKILL", "100.00", "20.00", "10.00")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不可与优惠券叠加");
    }

    private QuoteServiceImpl service() { return new QuoteServiceImpl(quoteMapper, pricingRuleService, new ObjectMapper()); }
    private PricingRuleVersion rule(Long version) { PricingRuleVersion rule = new PricingRuleVersion(); rule.setVersion(version); rule.setMaxDiscountRate(BigDecimal.ONE); return rule; }
    private QuoteRequest request(String scene, String original, String activity, String coupon) { QuoteRequest request = new QuoteRequest(); request.setUserId(1L); request.setMerchantId(2L); request.setScene(scene); request.setOriginalAmount(new BigDecimal(original)); request.setActivityDiscountAmount(new BigDecimal(activity)); request.setCouponDiscountAmount(new BigDecimal(coupon)); return request; }
}
