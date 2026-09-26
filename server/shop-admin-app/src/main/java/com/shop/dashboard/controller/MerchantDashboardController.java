package com.shop.dashboard.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.dto.MerchantWorkbenchVO;
import com.shop.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;
import com.shop.dashboard.dto.SettlementAnalysisVO;

@RestController
@RequestMapping("/api/merchant/dashboard")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @RequirePermission("merchant:dashboard:view")
    public ApiResult<DashboardOverviewVO> overview() {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ApiResult.success(dashboardService.merchantOverview(user.getMerchantId()));
    }

    @GetMapping("/workbench")
    @RequirePermission("merchant:dashboard:view")
    public ApiResult<MerchantWorkbenchVO> workbench() {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ApiResult.success(dashboardService.merchantWorkbench(user.getMerchantId()));
    }

    @GetMapping("/settlement-analysis")
    @RequirePermission("merchant:settlement:view")
    public ApiResult<SettlementAnalysisVO> settlementAnalysis(@RequestParam(required = false) LocalDateTime from,
                                                               @RequestParam(required = false) LocalDateTime to) {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        return ApiResult.success(dashboardService.settlementAnalysis(user.getMerchantId(), from, to));
    }
}
