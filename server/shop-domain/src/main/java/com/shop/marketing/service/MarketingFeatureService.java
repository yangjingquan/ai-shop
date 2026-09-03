package com.shop.marketing.service;

import com.shop.marketing.dto.MarketingFeatureVO;
import com.shop.marketing.enums.MarketingActivityCode;

import java.util.List;

public interface MarketingFeatureService {
    List<MarketingFeatureVO> list(Long merchantId);

    List<MarketingFeatureVO> listEnabled(Long merchantId);

    boolean isEnabled(Long merchantId, MarketingActivityCode code);

    void assertEnabled(Long merchantId, MarketingActivityCode code);

    void initializeMerchant(Long merchantId);

    void update(Long merchantId, String code, Integer enabled, Long operatorId);
}
