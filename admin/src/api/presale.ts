import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface PresaleSku {
  id?: number
  productId: number | null
  skuId: number | null
  productName?: string
  mainImage?: string
  specText?: string
  originalPrice?: number
  finalPrice: number | null
  depositAmount: number | null
  depositDeductionAmount: number | null
  balanceAmount?: number | null
  finalAmount?: number
  userLimit: number
  stock?: number
  depositCount?: number
  balanceSoldCount?: number
  skuOptions?: Array<{ id: number; specText?: string; price?: number; stock?: number }>
}

export interface PresaleActivity {
  id?: number
  name?: string
  activityName?: string
  description?: string
  bannerImage?: string
  depositStartAt: string
  depositEndAt: string
  balanceStartAt: string
  balanceEndAt: string
  expectedShipAt: string
  status?: number
  statusText?: string
  phaseText?: string
  autoCloseExpired: number
  depositRefundRule?: string
  merchantBreachRule?: string
  skus: PresaleSku[]
}

export interface PresaleStats {
  depositOrderCount: number
  balancePaidOrderCount: number
  overdueOrderCount: number
  refundOrderCount: number
  finalAmount: number
}

export const presaleApi = {
  page: (params: { page: number; size: number }) => request.get<unknown, PageResult<PresaleActivity>>('/api/merchant/presales', { params }),
  get: (id: number) => request.get<unknown, PresaleActivity>(`/api/merchant/presales/${id}`),
  create: (data: PresaleActivity) => request.post<unknown, number>('/api/merchant/presales', data),
  update: (id: number, data: PresaleActivity) => request.put<unknown, void>(`/api/merchant/presales/${id}`, data),
  stats: (id: number) => request.get<unknown, PresaleStats>(`/api/merchant/presales/${id}/stats`),
}
