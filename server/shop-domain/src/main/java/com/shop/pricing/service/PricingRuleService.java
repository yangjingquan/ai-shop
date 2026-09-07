package com.shop.pricing.service;

import com.shop.pricing.dto.PricingRuleVersionRequest;
import com.shop.pricing.dto.PricingRuleVersionVO;
import com.shop.pricing.entity.PricingRuleVersion;

public interface PricingRuleService {
    PricingRuleVersion active();
    PricingRuleVersionVO activeView();
    PricingRuleVersionVO publish(Long operatorId, PricingRuleVersionRequest request);
    boolean allowsZeroPay(String scene);
    boolean allowsCombination(String primaryRule, String secondaryRule);
}
