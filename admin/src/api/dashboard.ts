import request from '@/utils/request'

export interface DashboardOverview {
  merchantCount?: number
  activeMerchantCount?: number
  orderCountToday: number
  createdOrderCountToday?: number
  paidOrderCountToday: number
  paidAmountToday: number
  refundAmountToday: number
  netAmountToday: number
  pendingShipCount: number
  pendingRefundCount: number
  onSaleProductCount: number
  lowStockSkuCount: number
}

export interface DashboardTrendRow {
  date: string
  paidOrderCount: number
  paidAmount: number
  refundAmount: number
  netAmount: number
}

export const dashboardApi = {
  adminOverview: () =>
    request.get<unknown, DashboardOverview>('/api/admin/dashboard/overview'),
  adminTrend: (days = 30) =>
    request.get<unknown, DashboardTrendRow[]>('/api/admin/dashboard/trend', { params: { days } }),
  merchantOverview: () =>
    request.get<unknown, DashboardOverview>('/api/merchant/dashboard/overview'),
}
