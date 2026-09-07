import request from '@/utils/request'

export interface PricingRuleVersion {
  version: number
  matrixJson: string
  zeroPayWhitelistJson: string
  maxDiscountRate: number
}

export const pricingRuleApi = {
  active: () => request.get<unknown, PricingRuleVersion>('/api/admin/marketing-rules/active'),
  publish: (data: Omit<PricingRuleVersion, 'version'> & { reason?: string }) =>
    request.put<unknown, PricingRuleVersion>('/api/admin/marketing-rules/publish', data),
}
