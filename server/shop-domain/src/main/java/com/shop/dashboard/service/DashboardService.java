package com.shop.dashboard.service;

import com.shop.dashboard.dto.DashboardOverviewVO;
import com.shop.dashboard.dto.DashboardTrendVO;

import java.util.List;

public interface DashboardService {

    DashboardOverviewVO adminOverview();

    DashboardOverviewVO merchantOverview(Long merchantId);

    List<DashboardTrendVO> adminTrend(int days);
}
