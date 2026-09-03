import request from '@/utils/request'

export interface MarketingFeature {
  code: string
  name: string
  description: string
  enabled: number
  implemented: boolean
  frontendPath: string
  sort: number
}

export const marketingApi = {
  features: () => request.get<unknown, MarketingFeature[]>('/api/merchant/marketing/features'),
  updateFeature: (code: string, enabled: number) =>
    request.put<unknown, void>(`/api/merchant/marketing/features/${code}`, { enabled }),
}

export interface CouponTemplate {
  id: number
  name: string
  type: number
  amount: number
  thresholdAmount: number
  totalStock: number
  receivedCount: number
  usedCount: number
  perUserLimit: number
  validityDays: number
  validFrom?: string | null
  validTo?: string | null
  scopeType: number
  scopeIds?: number[]
  excludeActivityGoods: number
  stackable: number
  status: number
  statusText: string
}

export interface CouponTemplatePayload {
  name: string
  amount: number
  thresholdAmount: number
  totalStock: number
  perUserLimit: number
  validityDays: number
  validFrom?: string | null
  validTo?: string | null
  scopeType: number
  scopeIds: number[]
  excludeActivityGoods: number
  stackable: number
  status: number
}

export const couponTemplateApi = {
  list: () => request.get<unknown, CouponTemplate[]>('/api/merchant/marketing/coupons/templates'),
  create: (data: CouponTemplatePayload) =>
    request.post<unknown, { id: number }>('/api/merchant/marketing/coupons/templates', data),
  update: (id: number, data: CouponTemplatePayload) =>
    request.put<unknown, void>(`/api/merchant/marketing/coupons/templates/${id}`, data),
  status: (id: number, status: number) =>
    request.put<unknown, void>(`/api/merchant/marketing/coupons/templates/${id}/status`, null, { params: { status } }),
}
