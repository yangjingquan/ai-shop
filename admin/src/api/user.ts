import request from '@/utils/request'
import type { PageResult } from '@/api/merchant'

export interface AdminUserRow {
  id: number
  merchantId?: number | null
  merchantName?: string
  nickname?: string
  avatar?: string
  phone?: string
  createdAt?: string
  lastLoginAt?: string
}

export const adminUserApi = {
  page: (params: { page: number; size: number; merchantId?: number; keyword?: string }) =>
    request.get<unknown, PageResult<AdminUserRow>>('/api/admin/users/page', { params }),
}
