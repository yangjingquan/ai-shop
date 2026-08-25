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
  shipNo?: string
  shipTime?: string
  cancelTime?: string
  cancelReason?: string
  refundStatus?: number
  refundReason?: string
  refundRejectReason?: string
  address?: { receiver?: string; phone?: string; region?: string; detail?: string }
  items?: AdminOrderItem[]
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
}

export const adminOrderApi = {
  page: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<AdminOrderRow>>('/api/admin/orders/page', { params }),
  detail: (orderNo: string) =>
    request.get<unknown, AdminOrderDetail>(`/api/admin/orders/${orderNo}`),
  refunds: (params: Record<string, unknown>) =>
    request.get<unknown, PageResult<AdminRefundRow>>('/api/admin/refunds/page', { params }),
}
