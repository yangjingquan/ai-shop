package com.shop.merchant.service;

import com.shop.common.response.PageResult;
import com.shop.merchant.dto.CreateMerchantRequest;
import com.shop.merchant.dto.MerchantSelfVO;
import com.shop.merchant.dto.MerchantVO;
import com.shop.merchant.dto.UpdateMerchantRequest;
import com.shop.merchant.dto.UpdateMerchantSelfRequest;

public interface MerchantManagementService {

    Long createMerchant(CreateMerchantRequest req, Long adminUserId);

    PageResult<MerchantVO> listMerchants(int page, int size, String keyword);

    MerchantVO getMerchant(Long id);

    void updateMerchant(Long id, UpdateMerchantRequest req);

    void setStatus(Long id, int status);

    void resetPassword(Long id, String newPassword);

    MerchantSelfVO getSelf(Long merchantId);

    void updateSelf(Long merchantId, UpdateMerchantSelfRequest req);
}
