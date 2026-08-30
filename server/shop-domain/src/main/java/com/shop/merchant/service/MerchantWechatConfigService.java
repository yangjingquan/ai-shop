package com.shop.merchant.service;

import com.shop.common.response.PageResult;
import com.shop.merchant.dto.MerchantWechatSettingsVO;
import com.shop.merchant.dto.UpdateWechatSettingsRequest;
import com.shop.merchant.entity.MerchantWechatConfig;

public interface MerchantWechatConfigService {

    MerchantWechatConfig getByMerchantId(Long merchantId);

    MerchantWechatConfig getRequiredByMerchantId(Long merchantId);

    MerchantWechatConfig getRequiredByAppId(String appId);

    void ensureConfig(Long merchantId);

    PageResult<MerchantWechatSettingsVO> page(int page, int size, String keyword);

    MerchantWechatSettingsVO getSettings(Long merchantId);

    void updateSettings(Long merchantId, UpdateWechatSettingsRequest request);
}
