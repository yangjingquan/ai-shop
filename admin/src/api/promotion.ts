import request from '@/utils/request'

export interface PromotionThreshold { thresholdAmount: number; reductionAmount?: number; discountRate?: number; discountCap?: number }
export interface PromotionActivity {
  id?: number; name: string; activityType: 'FULL_REDUCTION' | 'FULL_DISCOUNT'; priority: number; status: number;
  startAt: string; endAt: string; scopeType: number; stackNewUserCoupon: number; stackRepurchaseCoupon: number;
  showRecommendations: number; budgetAmount?: number | null; maxOrderCount?: number | null;
  productIds: number[]; categoryIds: number[]; excludedProductIds: number[]; recommendProductIds: number[]; thresholds: PromotionThreshold[];
  reservedBudget?: number; reservedOrderCount?: number; paidBudget?: number; paidOrderCount?: number;
}
export const promotionApi = {
  list: () => request.get<unknown, PromotionActivity[]>('/api/merchant/promotions'),
  create: (data: PromotionActivity) => request.post<unknown, number>('/api/merchant/promotions', data),
  update: (id: number, data: PromotionActivity) => request.put<unknown, void>(`/api/merchant/promotions/${id}`, data),
  status: (id: number, status: number) => request.put<unknown, void>(`/api/merchant/promotions/${id}/status`, { status }),
}
