package com.shop.dashboard.service;

import com.shop.dashboard.dto.DashboardOverviewVO;

public interface DashboardService {

    DashboardOverviewVO adminOverview();

    DashboardOverviewVO merchantOverview(Long merchantId);
}
