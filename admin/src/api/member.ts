import request from '@/utils/request'

export interface MemberProfileVO {
  id: number
  userId: number
  nickname?: string
  avatar?: string
  phone?: string
  status: number
  level?: number
  levelName?: string
  pointsBalance: number
  totalPoints: number
  joinedAt?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export const memberApi = {
  page: (params: { page: number; size: number; keyword?: string; level?: number }) =>
    request.get<unknown, PageResult<MemberProfileVO>>('/api/merchant/points/members', { params }),
}
