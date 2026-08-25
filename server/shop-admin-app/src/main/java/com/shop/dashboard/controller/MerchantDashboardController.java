package com.shop.dashboard.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/dashboard")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResult<DashboardOverviewVO> overview() {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ApiResult.success(dashboardService.merchantOverview(user.getMerchantId()));
    }
}
