import request from '@/utils/request'

export type MarketingTrigger = 'REGISTER' | 'ORDER_PAID' | 'BROWSE_NO_PURCHASE' | 'COUPON_EXPIRING' | 'DORMANT' | 'MEMBER_UPGRADED'

export interface MarketingJourney {
  id: number
  name: string
  triggerType: MarketingTrigger
  segmentId?: number | null
  delayMinutes: number
  couponTemplateId?: number | null
  notificationTitle?: string
  notificationContent?: string
  frequencyDays: number
  stopOnPaid: number
  status: number
  enrollmentCount: number
  successCount: number
  skippedCount: number
  couponCount: number
  notificationCount: number
  updatedAt?: string
}

export type MarketingJourneyPayload = Omit<MarketingJourney, 'id' | 'enrollmentCount' | 'successCount' | 'skippedCount' | 'couponCount' | 'notificationCount' | 'updatedAt'>

export const marketingJourneyApi = {
  list: () => request.get<unknown, MarketingJourney[]>('/api/merchant/marketing-journeys'),
  create: (data: MarketingJourneyPayload) => request.post<unknown, number>('/api/merchant/marketing-journeys', data),
  update: (id: number, data: MarketingJourneyPayload) => request.put<unknown, number>(`/api/merchant/marketing-journeys/${id}`, data),
  scan: (id: number) => request.post<unknown, void>(`/api/merchant/marketing-journeys/${id}/scan`),
}
