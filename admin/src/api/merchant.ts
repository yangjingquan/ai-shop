import request from '@/utils/request'

export interface MerchantVO {
  id: number
  merchantCode?: string
  name: string
  username?: string
  logo?: string
  description?: string
  address?: string
  contactName?: string
  contactPhone?: string
  wxAppId?: string
  wxSecretConfigured?: boolean
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface CreateMerchantPayload {
  name: string
  username: string
  password: string
  contactName?: string
  contactPhone?: string
  description?: string
  address?: string
  logo?: string
  wxAppId?: string
  wxSecret?: string
}

export type UpdateMerchantPayload = Omit<CreateMerchantPayload, 'username' | 'password'>

export const merchantApi = {
  list: (params: { page: number; size: number; keyword?: string }) =>
    request.get<unknown, PageResult<MerchantVO>>('/api/admin/merchants', { params }),
  get: (id: number) =>
    request.get<unknown, MerchantVO>(`/api/admin/merchants/${id}`),
  create: (data: CreateMerchantPayload) =>
    request.post<unknown, { id: number }>('/api/admin/merchants', data),
  update: (id: number, data: UpdateMerchantPayload) =>
    request.put<unknown, void>(`/api/admin/merchants/${id}`, data),
  setStatus: (id: number, status: number) =>
    request.put<unknown, void>(`/api/admin/merchants/${id}/status`, { status }),
}
