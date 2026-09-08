import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface AdminOrderRow {
  orderNo: string
  status: number
  statusText: string
  state?: number
  stateText?: string
  orderType?: number
  orderTypeText?: string
  fulfillmentMethod?: number
  fulfillmentMethodText?: string
  payAmount?: number
  merchantId?: number
  merchantName?: string
  createdAt?: string
}

export interface OrderDictionaryItem { code: number; text: string }
export interface OrderDictionary { orderTypes: OrderDictionaryItem[]; states: OrderDictionaryItem[]; fulfillmentMethods: OrderDictionaryItem[] }

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

export interface GroupRefundTaskRow {
  id: number
  groupId?: number
  orderNo: string
  refundApplicationId?: number
  status?: string
  statusText?: string
  retryCount?: number
  lastError?: string
  nextRetryAt?: string
  completedAt?: string
  createdAt?: string
  updatedAt?: string
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

export interface ReconciliationTask {
  taskNo: string
  taskType: string
  merchantId?: number
  rangeStart?: string
  rangeEnd?: string
  status: string
  affectedCount: number
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
}

export interface AfterSalesIntervention {
  id: number
  refundId: number
  status: string
  reason: string
  evidenceNote?: string
  resolution?: string
  createdAt?: string
}

export const adminOrderApi = {
  dictionary: () => request.get<unknown, OrderDictionary>('/api/admin/orders/dictionary'),
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
  groupRefundTasks: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<GroupRefundTaskRow>>('/api/merchant/group-buy/refund-tasks', { params }),
  payments: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<AdminPaymentRow>>('/api/admin/payments/page', { params }),
  previewReconciliation: (type: 'PAYMENT' | 'REFUND') =>
    request.get<unknown, ReconciliationTask>('/api/admin/reconciliation/preview', { params: { type } }),
  createReconciliation: (type: 'PAYMENT' | 'REFUND') =>
    request.post<unknown, ReconciliationTask>('/api/admin/reconciliation/tasks', null, { params: { type } }),
  reconciliationTasks: (type?: 'PAYMENT' | 'REFUND') =>
    request.get<unknown, ReconciliationTask[]>('/api/admin/reconciliation/tasks', { params: { type } }),
  openIntervention: (refundId: number, data: { reason: string; evidenceNote?: string }) =>
    request.post<unknown, AfterSalesIntervention>(`/api/admin/refunds/${refundId}/interventions`, data),
  interventions: (refundId: number) => request.get<unknown, AfterSalesIntervention[]>(`/api/admin/refunds/${refundId}/interventions`),
}
