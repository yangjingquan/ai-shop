package com.shop.pricing.service;

import com.shop.pricing.dto.QuoteRequest;
import com.shop.pricing.dto.QuoteResult;

public interface QuoteService {
    QuoteResult quote(QuoteRequest request);
    QuoteResult requireValid(Long userId, Long merchantId, String quoteId, Long ruleVersion, String scene);
}
