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

export interface BundleSku {
  id: number
  specText?: string
  price: number
  stock: number
  image?: string
}

export interface BundleItem {
  id?: number
  productId: number
  productName?: string
  mainImage?: string
  required?: number
  skus?: BundleSku[]
}

export interface BundleActivity {
  id?: number
  name: string
  mainProductId: number
  mainProductName?: string
  mainProductImage?: string
  mainSkus?: BundleSku[]
  discountAmount: number
  startAt: string
  endAt: string
  status: number
  statusText?: string
  active?: boolean
  items: BundleItem[]
}

export interface BundleActivityPayload {
  name: string
  mainProductId: number
  itemProductIds: number[]
  discountAmount: number
  startAt: string
  endAt: string
  status: number
}

export const bundleApi = {
  list: () => request.get<unknown, BundleActivity[]>('/api/merchant/bundles'),
  get: (id: number) => request.get<unknown, BundleActivity>(`/api/merchant/bundles/${id}`),
  create: (data: BundleActivityPayload) => request.post<unknown, number>('/api/merchant/bundles', data),
  update: (id: number, data: BundleActivityPayload) => request.put<unknown, void>(`/api/merchant/bundles/${id}`, data),
  disable: (id: number) => request.delete<unknown, void>(`/api/merchant/bundles/${id}`),
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
  image: string
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
  issueScene: 'NEW_USER' | 'REPURCHASE_AFTER_PAID'
  repurchaseTargetType: number
  repurchaseTargetIds?: number[]
  repurchaseMinOrderAmount: number
  repurchaseFirstPurchaseOnly: number
  repurchasePriority: number
  excludeActivityGoods: number
  stackable: number
  status: number
  statusText: string
}

export interface CouponTemplatePayload {
  name: string
  image: string
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
  issueScene: 'NEW_USER' | 'REPURCHASE_AFTER_PAID'
  repurchaseTargetType: number
  repurchaseTargetIds: number[]
  repurchaseMinOrderAmount: number
  repurchaseFirstPurchaseOnly: number
  repurchasePriority: number
  excludeActivityGoods: number
  stackable: number
  status: number
}

export type CouponIssueScene = 'NEW_USER' | 'REPURCHASE_AFTER_PAID'

export const couponTemplateApi = {
  list: (issueScene: CouponIssueScene = 'NEW_USER') =>
    request.get<unknown, CouponTemplate[]>('/api/merchant/marketing/coupons/templates', { params: { issueScene } }),
  create: (issueScene: CouponIssueScene, data: CouponTemplatePayload) =>
    request.post<unknown, { id: number }>('/api/merchant/marketing/coupons/templates', data, { params: { issueScene } }),
  update: (id: number, data: CouponTemplatePayload) =>
    request.put<unknown, void>(`/api/merchant/marketing/coupons/templates/${id}`, data),
  status: (id: number, status: number) =>
    request.put<unknown, void>(`/api/merchant/marketing/coupons/templates/${id}/status`, null, { params: { status } }),
}

export interface PointsRule { registerPoints: number; payAmountYuan: number; pointsPerYuan: number; signInPoints: number; validDays: number; deductionPerYuan: number; deductionMaxPoints: number; status: number }
export interface PointsProduct { id?: number; productId?: number | null; skuId?: number | null; couponTemplateId?: number | null; title: string; image?: string; pointsPrice: number; stock: number; perUserLimit: number; validFrom?: string | null; validTo?: string | null; status: number; physical?: boolean; redeemedCount?: number }
export interface MemberDayActivity { id?: number; name: string; dayOfMonth: number; startTime: string; endTime: string; doublePoints: number; couponTemplateId?: number | null; productScopeType: number; productScopeIdsJson?: string; stackable: number; status: number; active?: boolean; statusText?: string }
export const pointsApi = {
  rule: () => request.get<unknown, PointsRule>('/api/merchant/points/rule'), saveRule: (data: PointsRule) => request.put<unknown, void>('/api/merchant/points/rule', data),
  products: () => request.get<unknown, PointsProduct[]>('/api/merchant/points/products'), createProduct: (data: PointsProduct) => request.post<unknown, number>('/api/merchant/points/products', data), updateProduct: (id: number, data: PointsProduct) => request.put<unknown, number>(`/api/merchant/points/products/${id}`, data), deleteProduct: (id: number) => request.delete<unknown, void>(`/api/merchant/points/products/${id}`),
  memberDay: () => request.get<unknown, MemberDayActivity | null>('/api/merchant/points/member-day'), saveMemberDay: (data: MemberDayActivity) => request.put<unknown, void>('/api/merchant/points/member-day', data),
}
