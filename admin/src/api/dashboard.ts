import request from '@/utils/request'

export interface DashboardOverview {
  merchantCount?: number
  activeMerchantCount?: number
  orderCountToday: number
  paidAmountToday: number
  pendingShipCount: number
  pendingRefundCount: number
  onSaleProductCount: number
  lowStockSkuCount: number
}

export const dashboardApi = {
  adminOverview: () =>
    request.get<unknown, DashboardOverview>('/api/admin/dashboard/overview'),
  merchantOverview: () =>
    request.get<unknown, DashboardOverview>('/api/merchant/dashboard/overview'),
}
