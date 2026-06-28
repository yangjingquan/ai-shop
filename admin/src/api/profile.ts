import request from '@/utils/request'

export interface MerchantSelfVO {
  id: number
  name: string
  logo?: string
  description?: string
  address?: string
  contactName?: string
  contactPhone?: string
  createdAt?: string
  updatedAt?: string
}

export interface UpdateMerchantSelfPayload {
  logo?: string
  description?: string
  address?: string
  contactName?: string
  contactPhone?: string
}

export const profileApi = {
  get: () => request.get<unknown, MerchantSelfVO>('/api/merchant/profile'),
  update: (data: UpdateMerchantSelfPayload) =>
    request.put<unknown, void>('/api/merchant/profile', data),
}
