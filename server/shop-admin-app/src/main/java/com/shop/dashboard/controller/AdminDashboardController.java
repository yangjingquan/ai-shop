package com.shop.dashboard.controller;

import com.shop.common.response.ApiResult;
import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResult<DashboardOverviewVO> overview() {
        return ApiResult.success(dashboardService.adminOverview());
    }
}
