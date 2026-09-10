package com.shop.home.service;

import com.shop.home.dto.HomeModuleUpdateRequest;
import com.shop.home.dto.HomeModuleVO;
import java.util.List;

public interface HomeModuleConfigService {
    List<HomeModuleVO> list(Long merchantId);
    List<HomeModuleVO> listEnabled(Long merchantId);
    void update(Long merchantId, HomeModuleUpdateRequest request);
}
