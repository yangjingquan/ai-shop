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

export interface ReferralTier {
  inviteCount: number
  inviterCouponTemplateId: number | null
  couponName?: string
  couponAmount?: string
}

export interface ReferralCampaign {
  id?: number
  name: string
  shareTitle: string
  shareDescription?: string
  landingProductId?: number | null
  inviteeCouponTemplateId?: number | null
  tiers: ReferralTier[]
  startAt: string
  endAt: string
  maxDailyInvites: number
  maxTotalInvites: number
  status?: number
  statusText?: string
  active?: boolean
}

export interface ReferralCampaignPayload {
  name: string
  shareTitle: string
  shareDescription?: string
  landingProductId?: number | null
  inviteeCouponTemplateId?: number | null
  tiers: ReferralTier[]
  startAt: string
  endAt: string
  maxDailyInvites: number
  maxTotalInvites: number
  status: number
}

export interface ReferralStats { shares: number; opens: number; registrations: number; firstPurchases: number; rewardsIssued: number; rewardCost: number }
export interface ReferralRelation { id: number; inviterUserId: number; inviteeUserId: number; firstOrderNo?: string; status: number; statusText: string; boundAt?: string; completedAt?: string }
export interface ReferralReward { id: number; relationId: number; userId: number; role: string; tier: number; couponId?: number; rewardAmount?: number; triggerOrderNo?: string; status: number; statusText: string; revokeReason?: string }

export const referralApi = {
  list: () => request.get<unknown, ReferralCampaign[]>('/api/merchant/referral-campaigns'),
  get: (id: number) => request.get<unknown, ReferralCampaign>(`/api/merchant/referral-campaigns/${id}`),
  create: (data: ReferralCampaignPayload) => request.post<unknown, number>('/api/merchant/referral-campaigns', data),
  update: (id: number, data: ReferralCampaignPayload) => request.put<unknown, void>(`/api/merchant/referral-campaigns/${id}`, data),
  status: (id: number, status: number) => request.put<unknown, void>(`/api/merchant/referral-campaigns/${id}/status`, null, { params: { status } }),
  stats: (id: number) => request.get<unknown, ReferralStats>(`/api/merchant/referral-campaigns/${id}/stats`),
  relations: (id: number) => request.get<unknown, ReferralRelation[]>(`/api/merchant/referral-campaigns/${id}/relations`),
  freezeRelation: (id: number, relationId: number) => request.post<unknown, void>(`/api/merchant/referral-campaigns/${id}/relations/${relationId}/freeze`),
  rewards: (id: number) => request.get<unknown, ReferralReward[]>(`/api/merchant/referral-campaigns/${id}/rewards`),
  revokeReward: (id: number, rewardId: number, reason?: string) => request.post<unknown, void>(`/api/merchant/referral-campaigns/${id}/rewards/${rewardId}/revoke`, null, { params: { reason } }),
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
  newUserOnly: number
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
  newUserOnly: number
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
