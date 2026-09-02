import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface AdminOrderRow {
  orderNo: string
  status: number
  statusText: string
  orderType?: number
  payAmount?: number
  merchantId?: number
  merchantName?: string
  createdAt?: string
}

export interface AdminOrderItem {
  productName: string
  specText?: string
  quantity: number
  unitPrice?: number
  subtotal?: number
}

export interface AdminOrderDetail extends AdminOrderRow {
  totalAmount?: number
  freightAmount?: number
  discountAmount?: number
  payTransactionId?: string
  payTime?: string
  shipCompany?: string
  shipperCode?: string
  shipNo?: string
  shipTime?: string
  cancelTime?: string
  cancelReason?: string
  refundStatus?: number
  refundReason?: string
  refundRejectReason?: string
  address?: { receiver?: string; phone?: string; region?: string; detail?: string }
  items?: AdminOrderItem[]
  logistics?: AdminLogisticsTracking
}

export interface AdminLogisticsTrace {
  acceptTime?: string
  acceptStation?: string
  state?: string
  stateText?: string
}

export interface AdminLogisticsTracking {
  orderNo?: string
  shipCompany?: string
  shipperCode?: string
  shipNo?: string
  state?: string
  stateText?: string
  lastTime?: string
  lastContent?: string
  syncedAt?: string
  error?: string
  traces?: AdminLogisticsTrace[]
}

export interface AdminRefundRow {
  id: number
  orderNo: string
  userId?: number
  merchantId?: number
  merchantName?: string
  reason?: string
  status?: number
  statusText?: string
  rejectReason?: string
  createdAt?: string
  updatedAt?: string
  outRefundNo?: string
  wxRefundId?: string
  refundAmount?: number
  refundFailReason?: string
  refundTime?: string
  autoRefund?: number
  refundReconcileAt?: string
  refundReconcileAttempts?: number
  refundReconcileError?: string
  evidenceUrls?: string[]
  returnRequired?: number
  returnShipCompany?: string
  returnShipNo?: string
  returnShipTime?: string
  returnReceivedTime?: string
  returnReceiveNote?: string
}

export interface AdminPaymentRow {
  id: number
  orderNo: string
  transactionId: string
  amount: number
  merchantId?: number
  merchantName?: string
  orderStatus?: number
  payTime?: string
  createdAt?: string
  payReconcileAt?: string
  payReconcileAttempts?: number
  payReconcileError?: string
}

export const adminOrderApi = {
  page: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<AdminOrderRow>>('/api/admin/orders/page', { params }),
  detail: (orderNo: string) =>
    request.get<unknown, AdminOrderDetail>(`/api/admin/orders/${orderNo}`),
  cancel: (orderNo: string) =>
    request.post<unknown, void>(`/api/admin/orders/${orderNo}/cancel`),
  logistics: (orderNo: string, forceRefresh = false) =>
    request.get<unknown, AdminLogisticsTracking>(`/api/admin/orders/${orderNo}/logistics`, { params: { forceRefresh } }),
  refreshLogistics: (orderNo: string) =>
    request.post<unknown, AdminLogisticsTracking>(`/api/admin/orders/${orderNo}/logistics/refresh`),
  refunds: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<AdminRefundRow>>('/api/admin/refunds/page', { params }),
  payments: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<AdminPaymentRow>>('/api/admin/payments/page', { params }),
  reconcilePayments: () =>
    request.post<unknown, { paidCount: number }>('/api/admin/payments/reconcile'),
  reconcileRefunds: () =>
    request.post<unknown, { successCount: number }>('/api/admin/refunds/reconcile'),
}
