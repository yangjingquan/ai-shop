import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface BannerVO {
  id: number
  imageUrl: string
  linkType: number
  linkValue?: string
  sort: number
  status: number
  createdAt?: string
}

export interface BannerPayload {
  imageUrl: string
  linkType: number
  linkValue?: string
  sort: number
  status: number
}

export interface BannerPageQuery {
  page: number
  size: number
}

export const bannerApi = {
  page: (params: BannerPageQuery) =>
    request.get<unknown, PageResult<BannerVO>>('/api/merchant/banner/page', { params }),
  create: (data: BannerPayload) =>
    request.post<unknown, { id: number }>('/api/merchant/banner', data),
  update: (id: number, data: BannerPayload) =>
    request.put<unknown, void>(`/api/merchant/banner/${id}`, data),
  remove: (id: number) =>
    request.delete<unknown, void>(`/api/merchant/banner/${id}`),
}
