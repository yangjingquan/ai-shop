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
