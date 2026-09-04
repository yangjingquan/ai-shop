import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface SeckillSkuConfig {
  productId: number | null
  skuId: number | null
  activityPrice: number | null
  activityStock: number | null
  userLimit: number
  skuOptions?: Array<{ id: number; specText?: string; price?: number; stock?: number }>
}

export interface SeckillSessionConfig {
  name: string
  startAt: string
  endAt: string
  sort: number
  skus: SeckillSkuConfig[]
}

export interface SeckillActivity {
  id?: number
  activityName?: string
  name?: string
  description?: string
  preheatAt?: string | null
  status?: number
  statusText?: string
  sessions?: SeckillSessionConfig[]
}

export const seckillApi = {
  page: (params: { page: number; size: number }) =>
    request.get<unknown, PageResult<SeckillActivity>>('/api/merchant/seckill', { params }),
  get: (id: number) => request.get<unknown, SeckillActivity>(`/api/merchant/seckill/${id}`),
  create: (data: SeckillActivity) => request.post<unknown, number>('/api/merchant/seckill', data),
  update: (id: number, data: SeckillActivity) => request.put<unknown, void>(`/api/merchant/seckill/${id}`, data),
}
